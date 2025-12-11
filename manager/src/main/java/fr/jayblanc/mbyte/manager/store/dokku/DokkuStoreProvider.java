package fr.jayblanc.mbyte.manager.store.dokku;

import com.jcraft.jsch.*;
import fr.jayblanc.mbyte.manager.store.StoreProvider;
import fr.jayblanc.mbyte.manager.store.StoreProviderConfig;
import fr.jayblanc.mbyte.manager.store.StoreProviderException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class DokkuStoreProvider implements StoreProvider {

    private static final Logger LOGGER = Logger.getLogger(DokkuStoreProvider.class.getName());
    private static final String NAME = "dokku";

    @Inject StoreProviderConfig storeConfig;
    @Inject DokkuStoreProviderConfig config;

    private JSch jsch;

    @PostConstruct
    public void init() {
        if ( !storeConfig.provider().equals(NAME) ) {
            LOGGER.log(Level.INFO, "DokkuStoreProvider not activated (provider is " + storeConfig.provider() + ")");
            return;
        }
        try {
            JSch.setConfig("StrictHostKeyChecking", "no");
            byte[] dokkuKey = IOUtils.resourceToByteArray("/ssh/manager_rsa");
            byte[] dokkuPubkey = IOUtils.resourceToByteArray("/ssh/manager_rsa.pub");
            byte[] knownHosts = IOUtils.resourceToByteArray("/ssh/known_hosts");
            jsch = new JSch();
            jsch.addIdentity("dokku", dokkuKey, dokkuPubkey, null);
            jsch.setKnownHosts(new ByteArrayInputStream(knownHosts));
        } catch (IOException | JSchException e) {
            LOGGER.log(Level.SEVERE, "unable to start DokkuStoreProvider", e);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<String> listAllStores() throws StoreProviderException {
        LOGGER.log(Level.INFO, "[dokku] Listing store apps");
        String command = "apps:list";
        StringBuffer output = new StringBuffer();
        try {
            int status = execute("dokku", command, output);
            if ( status != 0 ) {
                throw new StoreProviderException("unable to list apps: " + output);
            }
            return Arrays.stream(output.toString().split("\\r?\\n")).filter(s -> !s.startsWith("=====>")).collect(Collectors.toList());
        } catch ( IOException | JSchException e ) {
            throw new StoreProviderException("unable to list apps", e);
        }
    }

    /*
    ssh dokku apps:create <pseudo>
    ssh dokku postgres:create <pseudo>-db
    ssh dokku postgres:link <appname>-db <pseudo>
    docker image save <appname>/filestore:24.2.1-SNAPSHOT | ssh dokku git:load-image <appname> <appname>/filestore:24.2.1-SNAPSHOT "Jayblanc" "jayblanc@gmail.com"
    ssh dokku storage:ensure-directory --chown heroku <appname>-data
    ssh dokku storage:mount <appname> /var/lib/dokku/data/storage/<appname>-data:/opt/jboss/filestore
    ssh dokku config:set <appname> \
          WILDFLY_ADMIN_PASSWORD=filestore \
          FILESTORE_OWNER=<appname> \
          FILESTORE_CONSUL_HOST=registry.miage22.jayblanc.fr \
          DB_USER=postgres \
          DB_PASS=93427d9d7ad9d97ef8bb4e299d1df5a8 \
          DB_HOST=dokku-postgres-<appname>-db \
          DB_PORT=5432 \
          DB_NAME=<appname>_db \
          OIDC_PROVIDER_URL=http://auth.miage23.jayblanc.fr/realms/Miage.23 \
          OIDC_CLIENT_ID=filestore \
          FILESTORE_HOME=/opt/jboss/filestore \
    ssh dokku ports:add <appname> http:80:8080
    * */

    @Override
    public String createStore(String id, String owner, String name) throws StoreProviderException {
        LOGGER.log(Level.INFO, "[dokku] Creating new store apps");
        List<String> cmds = new ArrayList<>();
        cmds.add("apps:create " + owner);
        cmds.add("postgres:create " + owner + "-db --password password");
        cmds.add("postgres:link " + owner + "-db " + owner);
        cmds.add("storage:ensure-directory --chown heroku " + owner + "-data");
        cmds.add("storage:mount " + owner + " /var/lib/dokku/data/storage/" + owner + "-data:/opt/jboss/filestore");
        cmds.add("config:set " + owner + " WILDFLY_ADMIN_PASSWORD=filestore" +
                " OIDC_PROVIDER_URL=http://auth." + config.host() + "/realms/Miage.23" +
                " OIDC_CLIENT_ID=filestore" +
                " DB_USER=postgres" +
                " DB_PASS=password" +
                " DB_HOST=dokku-postgres-" + owner + "-db" +
                " DB_PORT=5432" +
                " DB_NAME=" + owner + "_db" +
                " FILESTORE_HOME=/opt/jboss/filestore" +
                " FILESTORE_CONSUL_HOST=registry." + config.host() +
                " FILESTORE_CONSUL_PORT=8500" +
                " FILESTORE_OWNER=" + owner +
                " FILESTORE_NAME=" +  name +
                " FILESTORE_ID=" + id +
                " FILESTORE_FQDN=" + owner + "." + config.host());
        cmds.add("ports:add " + owner + " http:80:8080");
        cmds.add("git:from-image " + owner + " " + config.image());

        try {
            StringBuffer output = new StringBuffer();
            for (String cmd : cmds) {
                LOGGER.log(Level.INFO, "[dokku] " + cmd);
                int status = execute("dokku", cmd, output);
                if (status != 0) {
                    throw new StoreProviderException("error during app creation: " + output);
                }
            }
            return output.toString();
        } catch ( IOException | JSchException e ) {
            throw new StoreProviderException("Unable to create app", e);
        }
    }

    @Override
    public String destroyStore(String name) throws StoreProviderException {
        LOGGER.log(Level.INFO, "[dokku] Deleting app");
        String cmd1 = "--force apps:destroy " + name;

        try {
            StringBuffer output = new StringBuffer();
            LOGGER.log(Level.INFO, "[dokku] {}", cmd1);
            int status = execute("dokku", cmd1, output);
            if ( status != 0 ) {
                throw new StoreProviderException("unable to delete app: " + output);
            }
            LOGGER.log(Level.INFO, "[output] {}", output.toString());
        } catch ( IOException | JSchException e ) {
            throw new StoreProviderException("Unable to delete app", e);
        }

        return "";
    }

    private int execute(String username, String command, StringBuffer output) throws JSchException, IOException {
        Session session = jsch.getSession(username, config.host(), config.port());
        session.connect();

        Channel channel = session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);
        channel.setInputStream(null);

        //((ChannelExec)channel).getErrStream(es);
        //((ChannelExec)channel).setOutputStream(os);

        InputStream in = channel.getInputStream();
        channel.connect();
        byte[] tmp = new byte[1024];
        int status = -1;
        while(true){
            while(in.available()>0){
                int i = in.read(tmp, 0, 1024);
                if ( i < 0 ) {
                    break;
                }
                output.append(new String(tmp, 0, i, Charset.defaultCharset()));
            }
            if( channel.isClosed() ) {
                if ( in.available() > 0 ) {
                    continue;
                }
                status = channel.getExitStatus();
                break;
            }
            try{
                Thread.sleep(1000);
            } catch (Exception ee) {
                //
            }
        }
        channel.disconnect();
        session.disconnect();
        return status;
    }

}


