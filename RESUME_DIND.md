# Passage au Docker-in-Docker (DinD) — Resume technique

## 1. Problematique : Pourquoi changer ?

### Niveau 1 — Architecture "simulee"

Dans la premiere version multi-serveur, les deux serveurs (`local` et `remote1`) utilisaient **le meme daemon Docker** (via `unix:///var/run/docker.sock`). Seul le repertoire de travail differait :

| Serveur | Docker Host | Workdir |
|---------|------------|---------|
| local | `unix:///var/run/docker.sock` | `/var/mbyte/stores` |
| remote1 | `unix:///var/run/docker.sock` | `/var/mbyte/stores2` |

Cela prouvait que le code multi-serveur fonctionnait (selection, health check, replication), mais **un seul daemon Docker** etait utilise. Le professeur a exige de "communiquer avec plusieurs daemon Docker" — le Niveau 1 ne satisfaisait pas cette exigence.

### Objectif du Niveau 2

Faire en sorte que le Manager communique avec **deux daemons Docker distincts** :
- Le daemon local du host (socket Unix)
- Un second daemon Docker isole (Docker-in-Docker, via TCP)

---

## 2. Difficultes techniques rencontrees

### Difficulte 1 — Filesystem isole

Le Manager creait des repertoires sur son propre filesystem (`Files.createDirectories()`) avant de creer les volumes Docker. Or, le DinD est un conteneur separe — le Manager n'a pas acces a son filesystem. L'appel echouait systematiquement pour les stores deployes sur le DinD.

### Difficulte 2 — Bind mount impossible

Les volumes Docker etaient crees en mode **bind mount** :
```java
client.createVolumeCmd()
    .withDriver("local")
    .withDriverOpts(Map.of("type", "none", "o", "bind", "device", "/path/on/host"))
    .exec();
```
Ce mode exige que le chemin existe **sur la machine ou tourne le daemon Docker**. Sur le DinD (daemon vierge), aucun chemin n'existait — la creation de volume echouait.

### Difficulte 3 — Reseau absent sur le DinD

Le code cherchait le reseau `mbyte.net` sur chaque daemon avant de creer les conteneurs. Ce reseau est cree par `docker-compose` et n'existe que sur le daemon local. Sur le DinD, `listNetworksCmd().withNameFilter("mbyte.net")` retournait vide — la creation de store echouait a l'etape 1/7.

### Difficulte 4 — Images Docker absentes

Le DinD est un daemon Docker vierge. Il ne possede aucune image Docker. Les images `postgres:latest` et `victor/store:25.1-SNAPSHOT` necessaires a la creation des stores n'etaient pas disponibles.

### Difficulte 5 — Routage Traefik

Traefik fonctionne avec le Docker provider et detecte les conteneurs **sur le daemon local uniquement**. Les conteneurs crees sur le DinD sont invisibles pour Traefik — ils ne sont pas routable via HTTP.

---

## 3. Solution implementee — DinD simplifie

### Architecture finale

```
                    +-------------------+
                    |    Traefik        |
                    |  (reverse proxy)  |
                    +--------+----------+
                             |
                    +--------+----------+
                    |     Manager       |
                    |  (Quarkus app)    |
                    +--------+----------+
                             |
              +--------------+--------------+
              |                             |
    +---------+----------+     +-----------+---------+
    |  Server "local"    |     |  Server "remote1"   |
    |  Docker daemon     |     |  Docker-in-Docker   |
    |  (socket Unix)     |     |  (tcp://server2:    |
    |  /var/run/docker   |     |         2375)       |
    |       .sock        |     |  Daemon separe      |
    +--------------------+     +---------------------+
```

### Changements effectues

#### A. Infrastructure (`docker-compose.yml`)

Ajout d'un service Docker-in-Docker :
```yaml
dind-server2:
  image: docker:27-dind
  hostname: server2
  container_name: mbyte.server2
  privileged: true
  environment:
    - DOCKER_TLS_CERTDIR=    # Pas de TLS (projet universitaire)
  networks:
    mbyte:
      ipv4_address: 172.25.0.10
```

#### B. Configuration (`application.properties`)

Le serveur remote1 pointe maintenant vers le daemon DinD :
```properties
manager.servers.servers.remote1.docker-host=tcp://server2:2375
manager.servers.servers.remote1.workdir-host=/data/stores
manager.servers.servers.remote1.workdir-local=/data/stores
```

#### C. Code Java (`DockerStoreProvider.java`)

Detection automatique du type de daemon :
```java
boolean isRemoteDaemon = server.getDockerHost().startsWith("tcp://");
```

Trois adaptations selon le type de daemon :

| Probleme | Daemon local | Daemon distant (DinD) |
|----------|-------------|----------------------|
| Filesystem | `Files.createDirectories()` | Skip (pas d'acces) |
| Volumes | Bind mount (`device: /path`) | Volume Docker natif (sans driverOpts) |
| Reseau | Doit exister (cree par compose) | Creation automatique si absent |

#### D. Chargement des images

Les images sont transferees du host vers le DinD apres le demarrage :
```bash
docker save victor/store:25.1-SNAPSHOT | docker exec -i mbyte.server2 docker load
docker save postgres:latest | docker exec -i mbyte.server2 docker load
```

#### E. Limitation acceptee

Les stores crees sur le DinD ne sont pas accessibles via Traefik (limitation du Docker provider). La demo montre la creation, le health check et la replication — le routage HTTP vers les stores DinD serait une amelioration future (via Consul Catalog provider).

---

## 4. Resultat

| Serveur | Daemon | Protocole | Statut |
|---------|--------|-----------|--------|
| local | Docker host | `unix:///var/run/docker.sock` | ONLINE |
| remote1 | Docker-in-Docker | `tcp://server2:2375` | ONLINE |

Le Manager communique avec **deux daemons Docker distincts**. Les stores peuvent etre crees, repliques et supprimes sur chacun des deux daemons. Le health check surveille les deux daemons toutes les 60 secondes.
