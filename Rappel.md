# Rappel - Projet MByte

## Description
MByte est un **clone de Google Drive** - une solution SaaS de stockage en ligne multi-tenant.
Chaque utilisateur peut créer un ou plusieurs "stores" (espaces de stockage) qui sont des conteneurs Docker isolés.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Traefik (Reverse Proxy)                  │
│                    http://localhost:8080                     │
└─────────────────────────────────────────────────────────────┘
           │              │              │
           ▼              ▼              ▼
    ┌──────────┐   ┌──────────┐   ┌──────────────┐
    │ Keycloak │   │ Manager  │   │ Store(s)     │
    │  (Auth)  │   │ (API)    │   │ (par user)   │
    └──────────┘   └──────────┘   └──────────────┘
           │              │              │
           ▼              ▼              ▼
    ┌──────────┐   ┌──────────┐   ┌──────────────┐
    │ Postgres │   │ Postgres │   │ Postgres +   │
    │ (auth)   │   │ (manager)│   │ Filesystem   │
    └──────────┘   └──────────┘   └──────────────┘
                        │
                        ▼
                  ┌──────────┐
                  │  Consul  │
                  │(registry)│
                  └──────────┘
```

---

## URLs de l'application

| Service | URL |
|---------|-----|
| Application principale | http://www.mbyte.fr |
| Keycloak (Auth) | http://auth.mbyte.fr |
| Consul (Registry) | http://registry.mbyte.fr |
| Traefik Dashboard | http://localhost:8080 |

---

## Commandes essentielles

### Build des images Docker
```bash
cd /mnt/c/M2/Docker2/TestMbyte/mbyte
mvn clean install -Dquarkus.container-image.build=true -DskipTests
```

### Lancer l'application
```bash
# Utiliser docker-compose (v1 avec tiret, pas v2 avec espace)
docker-compose down --remove-orphans
docker-compose up -d
```

### Vérifier les images
```bash
docker images | grep victor
```

### Logs
```bash
docker-compose logs -f manager
docker-compose logs -f keycloak
```

### Redémarrer un service après modification
```bash
docker-compose restart manager
```

---

## Problème de permissions Docker Socket

Si erreur de création de store:
```bash
# Vérifier les permissions
ls -la /var/run/docker.sock

# Corriger temporairement
sudo chmod 666 /var/run/docker.sock

# Solution permanente
sudo usermod -aG docker $USER
# Puis se déconnecter/reconnecter
```

---

## Configuration des images

| Fichier | Image utilisée |
|---------|----------------|
| `docker-compose.yml` | `victor/manager:25.1-SNAPSHOT` |
| `application.properties` ligne 7 | `victor/store:25.1-SNAPSHOT` |

**Important**: Les deux doivent utiliser le même préfixe (`victor/`)

---

## Fichiers clés

| Fichier | Description |
|---------|-------------|
| `docker-compose.yml` | Configuration Docker Compose |
| `auth/Dockerfile` | Image Keycloak |
| `manager/src/main/docker/Dockerfile.jvm` | Image Manager |
| `store/src/main/docker/Dockerfile.jvm` | Image Store |
| `manager/src/main/resources/application.properties` | Config Manager |
| `provisioning/init.sql` | Init base de données |

---

## Fonctionnalités implémentées

1. **Authentification SSO** via Keycloak (OIDC)
2. **Multi-tenant**: chaque utilisateur a son propre store isolé
3. **Multi-stores**: un utilisateur peut avoir plusieurs espaces de stockage
4. **Multi-serveurs**: les stores peuvent être créés sur différents serveurs Docker
5. **Gestion de fichiers**: upload, download, navigation, suppression
6. **Recherche full-text** dans les fichiers
7. **Service Discovery** via Consul

---

## Credentials par défaut

| Service | User | Password |
|---------|------|----------|
| Linux WSL | victor | azerty300 |
| Keycloak Admin | admin | password |
| PostgreSQL | mbyte | password |

---

## Erreurs courantes

### `KC_HTTP_ENABLED contains true, which is an invalid type`
→ Mettre entre guillemets: `KC_HTTP_ENABLED: "true"`

### `pull access denied for jerome/manager`
→ L'image n'existe pas, utiliser `victor/manager:25.1-SNAPSHOT`

### `docker compose unknown flag`
→ Utiliser `docker-compose` (avec tiret) au lieu de `docker compose` (avec espace)

---

## Volumes Docker

Les données sont stockées dans:
- `/var/mbyte/db` - Base de données PostgreSQL
- `/var/mbyte/stores` - Données des stores utilisateurs

Créer ces dossiers:
```bash
sudo mkdir -p /var/mbyte/db /var/mbyte/stores
sudo chmod -R 777 /var/mbyte
```
