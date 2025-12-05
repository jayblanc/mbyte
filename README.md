# The M[iage].Byte Project

## Project presentation

M[iage].Byte project is a clone of Google Drive.   
It is designed to understand component-based architecture and how to build a scalable application.  
In the final version, any user that creates an account on the system should gain access to a dedicated instance of the application. 
In the end, you'll have to individually work on a feature that will be integrated to the main project.

## Ramp-up

In order to enter the project and understand the different components, you will have to acquire knowledge on different technologies but also succeed in building and running the application locally and on the University servers. 

### Resources

**GitLab**  

https://gitlab.univ-lorraine.fr

**GitHub**

https://github.com/jayblanc/mbyte

**Discord**  

https://discord.gg/JrJeak3H

### Technologies used

Java, JakartaEE, JAX-RS, JPA, JTA, CDI, JUnit, Validation, Quarkus, Mockito, REST Assured, Consul, MOM, Dokku, Lucene, Solr, Tikka, Docker, OIDC, Keycloak, Postgres, Liquibase, Traeffik

https://jakarta.ee/  
https://jakarta.ee/specifications/  

https://quarkus.io/

https://www.consul.io/

https://www.keycloak.org/

https://www.postgresql.org/

### Global Architecture Understanding

The MByte project is a **Software as a Service (SaaS) online storage solution**. It is built using self-developed and existing pieces of software that evolve in a distributed environment.

The global architecture is composed of : 

- A **Reverse Proxy** that is the main entry point for web access (HTTP).
- An **Identity Provider (IdP)** that is responsible for the authentication of users in a centralized way : Keycloak. It implements OpenID Connect (OIDC) to ease integration with other high-level components.
- A **Relational Database** : PostgresSQL. It is used to persist the data of the applications. 
- A **Service Registry** : Consul. It is used to register and discover services in a distributed environment.
- The **Manager** application: It is the main entry point for the users. It is a web application that is responsible for the management of users and their stores.
- The **Store** application: It is the main application that is responsible for the management of files and folders. Each user has its own store.
- An optional **Single Page Application (SPA)** that acts as a unique GUI for users.

### Building modules and docker images

```bash
mvn clean install  -Dquarkus.container-image.build=true -DskipTests
```

### Configuring domain name resolution

In order to work properly, the application needs to resolve different domain names (those for the different services like registry, auth, manager, store, ...).
There are many options to have this working :
- Using /etc/hosts
- Using dnsmasq
- Using a local DNS server

For local development, the easiest way is to use /etc/hosts.

#### Using /etc/hosts

Add in /etc/hosts the following lines : 

```
127.0.0.1	auth.mbyte.fr www.mbyte.fr proxy.mbyte.fr registry.mbyte.fr sheldon.s.mbyte.fr
```

Note: The proxy GUI is accessible through http://proxy.mbyte.fr:8080

### Running the application

#### Using docker-compose

To start the application, you just have to use the provided docker-compose file.

```bash
docker compose  up --build
```

#### Using dev mode

In dev mode, you can run the manager or the store in a local JVM or in your IDE for easier update and debugging.

First stop the container you want to start locally (manager or store)

```bash
docker compose stop manager
```

Then start the module in dev mode. In the manager folder, run : 
```
export QUARKUS.TEST.CONTINUOUS-TESTING=disabled
export MANAGER.TOPOLOGY.HOST=172.25.0.10
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://172.25.0.11:5432/manager
export QUARKUS_HTTP_PORT=8088
mvn quarkus:dev 
```

Notice that we are using docker compose network IPs to connect to other services. 
You can repeat the same for the store module if needed.
You can also use the IDE to run the module in dev mode.

---

## Interesting links to learn more

### About REST and JAX-RS

- [REST API Tutorial](https://www.restapitutorial.com/)
- [JAX-RS Tutorial](https://www.javaguides.net/2018/09/jax-rs-tutorial.html)
- [Développer avec JAX-RS](https://mbaron.developpez.com/cours/soa/jaxrs/)
- [Spécification JAX-RS 3.1](https://jakarta.ee/specifications/restful-ws/3.1/jakarta-restful-ws-spec-3.1.pdf)
