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
127.0.0.1	auth.mbyte.fr www.mbyte.fr proxy.mbyte.fr registry.mbyte.fr sheldon.stores.mbyte.fr
```

Note: You will have to add more entries for each store you create. For example, if you create a store with identifier `teststore`, you will have to add the following line : 

``` 
127.0.0.1   teststore.stores.mbyte.fr
```

#### Using dnsmasq

Install Dnsmasq if not already installed

```bash
sudo apt-get install dnsmasq
``` 

Configure Dnsmasq for local domain resolution

Create file `/etc/dnsmasq.d/mbyte.conf` with the following content:

```bash
address=/mbyte.fr/127.0.0.1
address=/mbyte.fr/::1
```

Check file `/etc/dnsmask.conf` it should contain:

```bash
listen-address=127.0.0.1
bind-interfaces
``` 

Restart Dnsmasq service:

```bash
sudo systemctl restart dnsmasq
``` 

Note: Using dnsmasq allows you to avoid adding entries for each store you create.

### Creating working directories

For the application to work properly, you need to create some working directories on your host machine that will be mounted as volumes in the different containers.
The permissions for those directories must allow the containers to read and write data. Postgresql must also be able to create files in its data directory.

#### Global database directory

A global Postgresql container is used to store the data of the different applications (manager and auth). It must have a dedicated directory on the host machine.
This directory must hold permissions to allow Postgresql container user (999) to read and write data.

```bash
sudo mkdir -p /var/mbyte/db
sudo chmod -R 777 /var/mbyte
```

#### Stores directories

Dynamically created stores will have their data stored in the host directory `/var/local/mbyte/stores`.
Each store will have its own subdirectory named after the store identifier.
Two volumes will be created for stores: db and data. This will be handled by the manager directly, only the root folder needs to be created with proper permissions.

```bash
sudo mkdir -p /var/mbyte/stores
sudo chmod -R 777 /var/mbyte/stores
```

### Running the application

#### Using docker-compose

To start the application, you can use the provided docker-compose file:

```bash
docker compose up --build
```

#### Using dev mode

In dev mode, you can run the manager or the store in a local JVM or in your IDE for easier update and debugging.

First, stop the container you want to start locally (manager or store)

```bash
docker compose stop manager
```

Then start the module in dev mode. In the manager folder, run : 
```
export QUARKUS.TEST.CONTINUOUS-TESTING=disabled
export MANAGER.TOPOLOGY.HOST=172.25.0.3
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://172.25.0.4:5432/manager
export QUARKUS_HTTP_PORT=8088
mvn quarkus:dev 
```

Notice that we are using docker compose network IPs to connect to other services. 
You can repeat the same for the store module if needed.
You can also use the IDE to run the module in dev mode.

### Using the application

Visit http://www.mbyte.fr to access the main web interface. 
On the first visit you'll be redirected to Keycloak to create an account and login.
Create an account of your choice by clicking on 'register' and filling all required fields (email can be user@localhost).
Once logged in, you can create a store giving a name that will be used a DNS (aka http://mystorename.stores.mbyte.fr).
As you are the store owner, you can visit your store as the SSO is configured to automatically log you in.

Note: If you use /etc/hosts as a domain name resolver, remember to add an entry for your store. (127.0.0.1 mystorename.stores.mbyte.fr)

---

## License

This project is licensed under GPL-3.0. See file [LICENSE](LICENSE) for more information.

### Contributions

Extensions or modifications must use a GPL-3.0 license. Contributors must add their name to the copyright of modified files.

---

## Interesting links to learn more

### About REST and JAX-RS

- [REST API Tutorial](https://www.restapitutorial.com/)
- [JAX-RS Tutorial](https://www.javaguides.net/2018/09/jax-rs-tutorial.html)
- [Développer avec JAX-RS](https://mbaron.developpez.com/cours/soa/jaxrs/)
- [Spécification JAX-RS 3.1](https://jakarta.ee/specifications/restful-ws/3.1/jakarta-restful-ws-spec-3.1.pdf)
