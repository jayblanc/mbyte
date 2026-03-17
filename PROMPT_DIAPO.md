# Prompt pour Claude (navigateur) — Diaporama Multi-Server Deployment

Copier-coller le texte ci-dessous dans Claude :

---

Je dois completer un diaporama PowerPoint pour un projet universitaire (M2 MIAGE). Le sujet est "Multiple Server Deployment for stores" dans le projet MByte.

## Diapositives existantes (4 slides deja faites)

**Slide 1 - Titre** : "Multiple Server Deployment for stores" — KLODE Victor, BENAID Yassine, BALLOIR Gael

**Slide 2 - Contexte** : MByte aujourd'hui — Plateforme de gestion de stores Docker, Architecture centralisee sur un serveur unique, Fonctionne bien... pour l'instant

**Slide 3 - Le Probleme** : Capacite limitee (1 serveur = 1 plafond), Point de defaillance unique (si le serveur tombe, tout s'arrete), Pas de repartition geographique (latence), Scalabilite verticale uniquement (coute cher), Architecture centralisee sur un serveur unique

**Slide 4 - La Solution proposee** : Architecture multi-serveurs avec : ajout de serveurs a la volee via Server Registry centralise, repartition de charge intelligente (round-robin ou least-loaded), continuite de service avec health checks et failover, API transparente avec tracabilite serverId

## Slides a creer (suite du diaporama)

Cree les slides suivantes en gardant le meme style (titres en rouge/orange, bullet points concis, schema si pertinent). Chaque slide doit etre claire et pas trop chargee.

### Slide 5 — Architecture Technique (schema)

Representer l'architecture avec un schema textuel :

```
Utilisateur (navigateur)
      |
  [Traefik] — Reverse proxy, routage automatique
      |
  [Manager Quarkus] — Application principale
      |
  [ServerRegistry] — Registre des serveurs avec health check toutes les 60s
      |
  +---+---+
  |       |
[Server   [Server
 local]    remote1]
Docker     Docker
daemon     daemon
  |          |
Stores     Stores
```

Technologies : Quarkus, Docker Java API, PostgreSQL, Consul, Keycloak, Traefik, Liquibase

### Slide 6 — Composants developpes (code)

9 classes Java creees dans le package `server/` :

| Composant | Role |
|---|---|
| `Server.java` | Entite representant un serveur Docker (id, dockerHost, capacity, status...) |
| `ServerRegistry` / `ServerRegistryBean` | Registre centralise des serveurs + connexions Docker |
| `ServerConfig` | Configuration declarative dans application.properties |
| `ServerSelectionStrategy` | Interface de strategie de selection |
| `RoundRobinSelectionStrategy` | Distribution equitable des stores entre serveurs |
| `LeastLoadedSelectionStrategy` | Selection du serveur le moins charge |
| `ServerSelectionManager` | Orchestrateur : applique la strategie configuree |
| `ServersResource` | API REST : GET /api/servers, /api/servers/{id}/health |

Fichiers modifies : CoreServiceBean (+191 lignes), DockerStoreProvider (+243 lignes), ProfilesResource (+134 lignes), Store entity, StoreManager

### Slide 7 — Flux de creation d'un store (workflow)

Etapes du deploiement multi-serveur :

1. L'utilisateur clique "Create new store" et choisit un serveur (ou Automatique)
2. Le Manager appelle ServerSelectionManager
3. La strategie (round-robin ou least-loaded) selectionne le meilleur serveur
4. Le DockerStoreProvider se connecte au Docker daemon du serveur cible
5. 7 etapes Docker : reseau, volume DB, conteneur DB, demarrage DB, volume data, conteneur store, demarrage store
6. Le store est enregistre en BDD avec son serverId
7. Traefik route automatiquement vers le store via les labels Docker

### Slide 8 — Replication (deployer un store sur plusieurs serveurs)

Problematique : Comment assurer la resilience si un serveur tombe ?

Solution : Replication froide — deployer le meme store sur un autre serveur

Implementation :
- Changement de contrainte BDD : unique(owner, name) → unique(owner, name, serverId)
- Le meme store (meme nom, meme owner) peut exister sur des serveurs differents
- Bouton "Replicate" sur chaque carte de store dans l'UI
- Nouvel endpoint : POST /api/profiles/{id}/stores/{storeId}/replicate
- Verification anti-doublon : impossible de repliquer sur un serveur ou le store existe deja

### Slide 9 — Evolution de la base de donnees

4 changesets Liquibase :

| Changeset | Description |
|---|---|
| 1 | Schema initial (tables profile, store) |
| 2 | Support multi-store : contrainte unique (owner, name), index |
| 3 | Support multi-serveur : ajout colonne serverId, index |
| 4 | Support replication : contrainte unique (owner, name, serverId) |

Le champ `serverId` sur l'entite Store permet de savoir sur quel serveur vit chaque store.

### Slide 10 — Health Checks et Monitoring

Le ServerRegistry surveille les serveurs en temps reel :
- Health check toutes les 60 secondes via Docker API (`listNetworksCmd`)
- Statuts possibles : ONLINE, OFFLINE, DEGRADED, UNKNOWN
- Un serveur OFFLINE est automatiquement exclu de la selection
- API de monitoring : GET /api/servers → liste tous les serveurs avec leur statut
- GET /api/servers/{id}/health → statut de sante d'un serveur specifique
- GET /api/servers/{id}/stores/count → nombre de stores + capacite restante

### Slide 11 — Demo / Screenshots

Captures d'ecran de la demo :
1. Modal "Create new Store" avec dropdown serveur (Automatic, Local Docker, Docker Server 2)
2. Page profil avec des stores sur des serveurs differents (badges "local" et "remote1")
3. Modal "Replicate Store" avec selection du serveur cible
4. Logs montrant "Server local status: ONLINE / Server remote1 status: ONLINE"

(Note : je fournirai les screenshots separement)

### Slide 12 — Configuration Infrastructure

docker-compose.yml :
- Traefik (reverse proxy, port 80/443/8080)
- PostgreSQL (base de donnees)
- Consul (service registry)
- Keycloak (authentification OIDC)
- Manager (application Quarkus, avec acces au socket Docker)

application.properties :
- Serveur "local" : unix:///var/run/docker.sock, workdir /var/mbyte/stores
- Serveur "remote1" : unix:///var/run/docker.sock, workdir /var/mbyte/stores2
- Strategie : round-robin (configurable)

### Slide 13 — Difficultes rencontrees

1. **BindException: Permission denied** — Le conteneur manager (UID 185) ne pouvait pas acceder au socket Docker. Fix : `user: "0"` dans docker-compose.yml
2. **Health check echouant** — pingCmd et versionCmd echouaient. Fix : utiliser listNetworksCmd qui fonctionne
3. **Dropdown serveurs vide** — getEnabledServers() filtrait par ONLINE mais les serveurs etaient OFFLINE. Fix : filtrer uniquement par enabled
4. **Port 80 deja utilise** — Conflit avec des processus Windows. Fix : kill des processus et port forwarding WSL2
5. **Contrainte unique BDD** — Empeachait la replication. Fix : changeset Liquibase pour passer a (owner, name, serverId)

### Slide 14 — Conclusion et perspectives

Ce qui a ete realise :
- Architecture multi-serveurs fonctionnelle
- 2 strategies de selection (round-robin, least-loaded)
- Health checks automatiques toutes les 60s
- Replication de stores entre serveurs
- Interface web adaptee (dropdown serveur, bouton Replicate)
- 9 classes Java creees, ~600 lignes ajoutees

Perspectives d'amelioration :
- Replication "chaude" : synchronisation des donnees entre replicas
- Docker-in-Docker : vrais daemons Docker separes
- Ajout dynamique de serveurs via API (sans redemarrage)
- Dashboard d'administration des serveurs
- Metriques de performance par serveur (Prometheus/Grafana)

### Slide 15 — Questions ?

"Merci de votre attention" — KLODE Victor, BENAID Yassine, BALLOIR Gael

---

## Instructions de style

- Garder le meme theme PowerPoint (fond blanc, titres rouge/orange)
- Schemas en ASCII ou avec des boites simples
- Pas plus de 6-7 bullet points par slide
- Utiliser des icones/emojis si pertinent (mais rester professionnel)
- Police lisible, taille minimum 18pt pour le contenu
