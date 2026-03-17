# Guide de demonstration — Multi-Server Deployment & Replication

## Lancement du projet

### 1. Ouvrir un terminal Ubuntu (WSL2)

```bash
cd /mnt/c/M2/Docker2/TestMbyte/mbyte
```

### 2. Compiler et construire l'image Docker

```bash
mvn clean install -Dquarkus.container-image.build=true -DskipTests
```

Cette commande compile le projet Java (Quarkus) et construit l'image Docker `victor/manager:25.1-SNAPSHOT`. Compter environ 3 minutes.

### 3. Demarrer l'infrastructure

```bash
docker-compose down --remove-orphans && docker-compose up -d
```

Cela demarre tous les services : Traefik (proxy), PostgreSQL (BDD), Consul (registry), Keycloak (authentification), le Manager, et le **Docker-in-Docker (DinD)** comme second daemon Docker.

### 4. Charger les images Docker dans le DinD

Le DinD est un daemon Docker vierge. Il faut y charger les images necessaires :

```bash
# Attendre que le DinD soit pret (~10 secondes)
sleep 10
# Charger l'image store et PostgreSQL dans le DinD
docker save victor/store:25.1-SNAPSHOT | docker exec -i mbyte.server2 docker load
docker save postgres:latest | docker exec -i mbyte.server2 docker load
```

Verifier que les images sont bien chargees :
```bash
docker exec mbyte.server2 docker images
```

### 5. Verifier que tout est demarre

Attendre environ 30 secondes puis :

```bash
docker logs mbyte.manager 2>&1 | grep -i "server.*status"
```

On doit voir :
```
Server local status: ONLINE
Server remote1 status: ONLINE
```

Le serveur **local** utilise le daemon Docker du host (via socket Unix).
Le serveur **remote1** utilise le daemon DinD (via `tcp://server2:2375`).

### 6. Configurer le fichier hosts (Windows)

Ajouter dans `C:\Windows\System32\drivers\etc\hosts` (en administrateur) :
```
127.0.0.1 www.mbyte.fr
127.0.0.1 auth.mbyte.fr
127.0.0.1 registry.mbyte.fr
```

### 7. Configurer le port forwarding WSL2 (si necessaire)

Si les sites ne sont pas accessibles depuis Windows, executer dans un **PowerShell administrateur** :
```powershell
netsh interface portproxy add v4tov4 listenport=80 listenaddress=127.0.0.1 connectport=80 connectaddress=$(wsl hostname -I | ForEach-Object { $_.Trim().Split(' ')[0] })
```

## Liens utiles

| Service | URL |
|---|---|
| Interface MByte (Manager) | http://www.mbyte.fr |
| Keycloak (Authentification) | http://auth.mbyte.fr |
| Consul (Service Registry) | http://registry.mbyte.fr |
| Traefik Dashboard | http://localhost:8080 |

## Compte utilisateur

| Champ | Valeur |
|---|---|
| Login | `Victor3` |
| Mot de passe | `Victor3` |
| Email | `Victor3@gmail.com` |

Se connecter sur http://www.mbyte.fr avec ces identifiants. Apres connexion, on arrive sur la page **"Manage your profile"**.

---

## 1. Creer un store sur un serveur specifique

1. Cliquer sur **"+ Create new store"** (bouton bleu en haut de la page profil)
2. Dans **"Store name"**, entrer un nom (ex: `Victor1`)
3. Dans le dropdown **"Server"**, selectionner **"Local Docker (local)"**
4. Cliquer **"Create"**
5. La page se recharge — une carte apparait avec le store, affichant le badge **"local"** dans la ligne **Server**

## 2. Creer un store sur le second serveur

1. Cliquer a nouveau sur **"+ Create new store"**
2. Dans **"Store name"**, entrer un autre nom (ex: `Victor2`)
3. Dans le dropdown **"Server"**, selectionner **"Docker Server 2 (remote1)"**
4. Cliquer **"Create"**
5. La page se recharge — une seconde carte apparait avec le badge **"remote1"**

## 3. Creer un store en mode automatique (load balancing)

1. Cliquer sur **"+ Create new store"**
2. Dans **"Store name"**, entrer un nom (ex: `Victor3`)
3. Laisser le dropdown **"Server"** sur **"Automatic (load balanced)"**
4. Cliquer **"Create"**
5. Le systeme choisit automatiquement le serveur (round-robin ou least-loaded) — verifier sur la carte quel serveur a ete attribue

## 4. Repliquer un store sur un autre serveur

1. Sur la carte d'un store existant (ex: `Victor1` qui est sur **local**), cliquer le bouton **"Replicate"** (bleu, icone copie)
2. Un modal **"Replicate Store"** s'ouvre, affichant le nom du store
3. Dans le dropdown **"Target Server"**, selectionner un serveur different (ex: **"Docker Server 2 (remote1)"**)
4. Cliquer **"Replicate"**
5. La page se recharge — deux cartes avec le meme nom (`Victor1`) apparaissent, sur des serveurs differents (**local** et **remote1**)

## 5. Supprimer un store

1. Sur une carte de store, cliquer le bouton **"Delete"** (rouge, icone poubelle)
2. Un modal **"Delete Store"** demande confirmation
3. Cliquer **"Delete"** pour confirmer
4. La page se recharge — le store a disparu

---

## Ce que cela demontre

| Fonctionnalite | Ce qu'elle prouve |
|---|---|
| Dropdown serveur a la creation | Le manager connait tous les serveurs disponibles |
| Badge serveur sur chaque carte | Chaque store est associe a un serveur specifique (persiste en BDD) |
| Mode automatique | Le load balancer distribue les stores entre serveurs |
| Replication | Un meme store peut exister sur plusieurs serveurs (resilience) |
| Health check toutes les 60s | Le registry surveille l'etat des serveurs en temps reel |
| **Deux daemons Docker separes** | Le serveur local utilise le socket Unix, le serveur remote1 utilise un vrai daemon DinD via TCP |
| Conteneurs sur le DinD | `docker exec mbyte.server2 docker ps` montre les conteneurs crees sur le daemon distant |

---

## Architecture technique

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

### Composants cles

- **ServerRegistry** : Registre central des serveurs Docker, avec health check toutes les 60 secondes
- **ServerConfig** : Configuration des serveurs dans `application.properties`
- **StoreManager** : Selection du serveur pour un nouveau store (round-robin / least-loaded)
- **DockerStoreProvider** : Creation/suppression de conteneurs Docker sur le serveur cible
- **TopologyService** : Enregistrement des stores dans Consul pour le routage Traefik

### Fichiers de configuration

- `manager/src/main/resources/application.properties` : Declaration des serveurs
- `docker-compose.yml` : Infrastructure Docker (volumes, reseaux, services)
- `manager/src/main/resources/db/changeLog.xml` : Schema BDD (contrainte unique owner+name+serverId)

---

## Verification en ligne de commande

### Verifier le statut des serveurs
```bash
docker logs mbyte.manager 2>&1 | grep -i "server.*status"
```

### Verifier les stores en base de donnees
```bash
docker exec mbyte.db psql -U mbyte -d mbyte -c "SELECT id, name, owner, serverid, status FROM store;"
```

### Verifier les conteneurs Docker crees pour les stores

Sur le daemon local :
```bash
docker ps --filter "name=mbyte." --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Sur le daemon DinD (remote1) :
```bash
docker exec mbyte.server2 docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Verifier les volumes et reseaux sur le DinD
```bash
# Volumes crees sur le DinD (volumes natifs, pas de bind mount)
docker exec mbyte.server2 docker volume ls

# Reseaux sur le DinD (mbyte.net cree automatiquement par le Manager)
docker exec mbyte.server2 docker network ls
```

### Verifier les images chargees dans le DinD
```bash
docker exec mbyte.server2 docker images
```
