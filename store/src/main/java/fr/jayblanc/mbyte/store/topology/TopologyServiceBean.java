package fr.jayblanc.mbyte.store.topology;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import fr.jayblanc.mbyte.store.topology.entity.Neighbour;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Startup
public class TopologyServiceBean implements TopologyService {

    private static final Logger LOGGER = Logger.getLogger(TopologyService.class.getName());

    @Inject TopologyConfig config;

    private volatile Consul consulClient;
    private volatile String serviceName;
    private volatile String instanceId;
    private volatile boolean registered = false;

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing topology bean");
        if (config.enabled()) {
            consulClient = Consul.builder().withHttps(config.https()).withHostAndPort(HostAndPort.fromParts(config.host(), config.port()))
                    .build();
            serviceName = "mbyte.store.".concat(config.service().name());
            instanceId = serviceName.concat(".1");
            Registration service = this.buildRegistration();
            consulClient.agentClient().register(service);
            try {
                consulClient.agentClient().pass(instanceId);
                registered = true;
                LOGGER.log(Level.INFO, "Instance registered with id=" + instanceId);
            } catch (NotRegisteredException e) {
                LOGGER.log(Level.WARNING, "Unable to checkin topology registration", e);
            }
        } else {
            LOGGER.log(Level.INFO, "Topology service is disabled");
        }
    }

    @PreDestroy
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping topology bean");
        if (registered) {
            consulClient.agentClient().deregister(instanceId);
            this.registered = false;
            LOGGER.log(Level.INFO, "Service instance unregistered for id=" + instanceId);
        }
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public List<Neighbour> list() {
        LOGGER.log(Level.INFO, "Listing all neighbours");
        if (!registered) {
            LOGGER.log(Level.WARNING, "Topology service is not registered, cannot list neighbours");
            return List.of();
        } else {
            CatalogClient catalog = consulClient.catalogClient();
            Map<String, List<String>> services =  catalog.getServices().getResponse();
            List<String> stores = services.keySet().stream().filter(key -> key.startsWith("miage24.store.")).toList();
            LOGGER.log(Level.INFO, "Found stores: " + stores);
            return stores.stream().flatMap(name -> consulClient.healthClient().getAllServiceInstances(name).getResponse().stream().map(Neighbour::build)).collect(Collectors.toList());
        }
    }

    @Override
    @Scheduled(every = "10s")
    public void checkin() {
        if (registered) {
            LOGGER.log(Level.FINEST, "Checkin store in topology");
            try {
                consulClient.agentClient().pass(instanceId);
            } catch (NotRegisteredException e) {
                LOGGER.log(Level.WARNING, "Error while trying to checkin service in topology", e);
                Registration service = this.buildRegistration();
                consulClient.agentClient().deregister(instanceId);
                this.registered = false;
                consulClient.agentClient().register(service);
                this.registered = true;
            }
        }
    }

    private Registration buildRegistration() {
        String fqdn = config.service().protocol().concat("://").concat(config.service().host()).concat((config.service().port()!=80)?":"+config.service().port():"");
        return ImmutableRegistration.builder()
                .id(instanceId)
                .name(serviceName)
                .address(config.service().host())
                .port(config.service().port())
                .addTags("fqdn.".concat(fqdn))
                .check(ImmutableRegCheck.builder().ttl(String.format("%ss", 30L)).deregisterCriticalServiceAfter(String.format("%sh",1)).build())
                .build();
    }


}
