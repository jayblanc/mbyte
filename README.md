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

### Detailed 

### Running the application

### Using dev mode

### Using a postman client

### Writing a test

### A REST Resource

- [REST API Tutorial](https://www.restapitutorial.com/)
- [JAX-RS Tutorial](https://www.javaguides.net/2018/09/jax-rs-tutorial.html)
- [Développer avec JAX-RS](https://mbaron.developpez.com/cours/soa/jaxrs/)
- [Spécification JAX-RS 3.1](https://jakarta.ee/specifications/restful-ws/3.1/jakarta-restful-ws-spec-3.1.pdf)

### A Component

data store

authentication

principles of boxing complexity

loose coupling

### Persisting information

### Notification

### Authentication

### Discovery of other stores

## Deploy 


