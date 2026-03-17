# 📚 GUIDE COMPLET DE DÉMARRAGE - M[iage].Byte

## 🚀 Démarrage rapide

### 1️⃣ Ouvrir WSL2/Ubuntu
```bash
# Dans PowerShell ou CMD Windows
wsl
```

### 2️⃣ Naviguer vers le projet
```bash
cd /mnt/c/M2/Docker2/TestMbyte/mbyte
```

### 3️⃣ Lancer l'application
```bash
docker-compose up
```

**⏱️ Temps de démarrage : 30-60 secondes**

Attendez de voir ces messages :
```
mbyte.auth    | ... Keycloak ... started in ...s. Listening on: http://0.0.0.0:80
mbyte.manager | ... manager ... started in ...s. Listening on: http://0.0.0.0:8080
```

### 4️⃣ Accéder à l'application
Ouvrez votre navigateur Windows : **http://www.mbyte.fr**

---

## 🔧 Commandes utiles

### Démarrer en arrière-plan (mode détaché)
```bash
docker-compose up -d
```

### Voir les logs en temps réel
```bash
docker-compose logs -f
```

### Voir les logs d'un service spécifique
```bash
docker-compose logs -f manager    # Logs du Manager
docker-compose logs -f keycloak   # Logs de Keycloak
docker-compose logs -f db         # Logs de PostgreSQL
```

### Vérifier l'état des conteneurs
```bash
docker-compose ps
```

### Arrêter l'application
```bash
docker-compose down
```

### Arrêter ET supprimer les volumes (reset complet)
```bash
docker-compose down -v
sudo rm -rf /var/mbyte/db/*
```

### Redémarrer un service spécifique
```bash
docker-compose restart manager
docker-compose restart keycloak
```

---

## 🌐 URLs et accès

| Service | URL | Description |
|---------|-----|-------------|
| **Application principale** | http://www.mbyte.fr | Interface utilisateur principale |
| **Keycloak Admin** | http://auth.mbyte.fr | Console admin d'authentification |
| **Consul UI** | http://registry.mbyte.fr | Service registry / découverte de services |
| **Traefik Dashboard** | http://localhost:8080 | Dashboard du reverse proxy |

### Identifiants par défaut

**Keycloak Admin :**
- URL : http://auth.mbyte.fr
- Username : `admin`
- Password : `password`

**Utilisateurs applicatifs :**
- À créer lors de la première connexion sur http://www.mbyte.fr
- Cliquez sur "Register" pour créer un compte
- Email peut être `user@localhost`

---

## 📝 Fichier hosts Windows

**Emplacement :** `C:\Windows\System32\drivers\etc\hosts`

**Contenu requis :**
```
127.0.0.1 www.mbyte.fr
127.0.0.1 auth.mbyte.fr
127.0.0.1 registry.mbyte.fr
127.0.0.1 sheldon.s.mbyte.fr
```

**⚠️ Important :** Si vous créez un nouveau store (ex: `monstore`), ajoutez :
```
127.0.0.1 monstore.stores.mbyte.fr
```

---

## 🔄 Processus complet de recréation (si problème)

### 1️⃣ Arrêter et nettoyer tout
```bash
cd /mnt/c/M2/Docker2/TestMbyte/mbyte
docker-compose down -v
sudo rm -rf /var/mbyte/db/*
sudo rm -rf /var/mbyte/stores/*
docker network prune -f
```

### 2️⃣ Recréer les répertoires
```bash
sudo mkdir -p /var/mbyte/db /var/mbyte/stores
sudo chmod -R 777 /var/mbyte
```

### 3️⃣ Relancer
```bash
docker-compose up
```

---

## 🛠️ Reconstruction des images (si modification du code)

### Si vous modifiez le code Manager ou Store
```bash
cd /mnt/c/M2/Docker2/TestMbyte/mbyte
mvn clean install -Dquarkus.container-image.build=true -DskipTests
docker-compose up
```

---

## 📊 Vérification de l'état des services

### Vérifier que tous les services sont UP
```bash
docker-compose ps
```

**Résultat attendu :**
```
     Name                   State              Ports
---------------------------------------------------------------
mbyte.auth       Up      8080/tcp, 8443/tcp, 9000/tcp
mbyte.db         Up      5432/tcp
mbyte.manager    Up      8080/tcp
mbyte.proxy      Up      0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
mbyte.registry   Up      8500/tcp
```

### Vérifier les images Docker
```bash
docker images | grep victor
```

**Résultat attendu :**
```
victor/manager    25.1-SNAPSHOT    ...    498MB
victor/store      25.1-SNAPSHOT    ...    538MB
```

---

## 🐛 Résolution de problèmes courants

### Problème : "Port already in use"
```bash
# Trouver quel processus utilise le port 80
netstat -ano | findstr :80

# Arrêter les anciens conteneurs
docker-compose down
```

### Problème : Keycloak ne démarre pas (erreur DB)
```bash
docker-compose down -v
sudo rm -rf /var/mbyte/db/*
docker-compose up
```

### Problème : "Network overlaps"
```bash
docker network ls
docker network rm mbyte_network
docker-compose up
```

### Problème : Manager ne peut pas créer de stores
Vérifiez les permissions :
```bash
ls -la /var/mbyte/
sudo chmod -R 777 /var/mbyte/stores
```

---

## 📦 Architecture

```
┌─────────────────────────────────────────────┐
│         Windows (localhost:80)              │
│              ↓ (via hosts)                  │
│         www.mbyte.fr                        │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Traefik (Reverse Proxy) - 172.25.0.2      │
│  Routes: www → manager, auth → keycloak     │
└─────────────────────────────────────────────┘
       ↓                    ↓
┌─────────────┐      ┌─────────────────┐
│  Manager    │      │  Keycloak       │
│  :8080      │←────→│  (Auth) :80     │
│  172.25.0.6 │      │  172.25.0.5     │
└─────────────┘      └─────────────────┘
       ↓                    ↓
┌──────────────────────────────────────┐
│  PostgreSQL :5432 - 172.25.0.4       │
│  DBs: manager, store, keycloak       │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│  Consul (Registry) - 172.25.0.3      │
│  Service Discovery                   │
└──────────────────────────────────────┘
```

---

## 💡 Conseils pratiques

1. **Toujours démarrer depuis WSL2** (pas Git Bash Windows)
2. **Laisser les logs ouverts** lors du premier démarrage pour surveiller
3. **Attendre 30-60 secondes** après `docker-compose up` avant d'accéder au site
4. **Utiliser `docker-compose up -d`** pour lancer en arrière-plan après avoir vérifié que tout fonctionne
5. **Sauvegarder ce guide** pour référence future

---

## 📋 Configuration utilisée

### Images Docker
- **Manager** : `victor/manager:25.1-SNAPSHOT`
- **Store** : `victor/store:25.1-SNAPSHOT`
- **PostgreSQL** : `postgres:15`
- **Keycloak** : `keycloak:26.4.7` (build custom depuis ./auth)
- **Traefik** : `traefik:v3.6`
- **Consul** : `hashicorp/consul:1.19`

### Réseau Docker
- Nom : `mbyte.net`
- Subnet : `172.25.0.0/24`

### Volumes
- `/var/mbyte/db` → Base de données PostgreSQL
- `/var/mbyte/stores` → Stockage des stores créés dynamiquement

---

**✅ Vous êtes prêt ! Bon développement sur M[iage].Byte !** 🚀
