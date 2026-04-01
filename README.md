# TP4 — Authentification et Master Key

## Note pedagogique importante

Ce mecanisme est **pedagogique**.

- Le serveur stocke les mots de passe avec un **chiffrement reversible (AES-GCM)**.
- La **Master Key** est injectee via variable d'environnement et ne doit jamais etre dans le code.
- **En industrie**, on eviterait le chiffrement reversible. On prefererait :
  - Un hash non reversible et adaptatif (argon2id, bcrypt)
  - Un protocole de type **SRP** (Secure Remote Password)
- **Risque** : si la Master Key est compromise, tous les mots de passe stockes peuvent etre dechiffres.

---

## Nouveautes TP4 par rapport a TP3

| Fonctionnalite | TP3 | TP4 |
|---|---|---|
| Source de la cle | `app.smk` dans application.properties | Variable d'environnement `APP_MASTER_KEY` |
| Cle dans le code | Oui (mauvais) | Non (correct) |
| Demarrage sans cle | Demarre quand meme | Refuse de demarrer |
| Format stockage | `Base64(iv+ciphertext)` | `v1:Base64(iv):Base64(ciphertext)` |
| CI/CD | GitHub Actions basique | CI complete avec blocage des merges |

---

## Principe du protocole

Ce serveur implemente une authentification forte par preuve HMAC.
**Le mot de passe ne circule jamais sur le reseau** (ni en clair, ni hache).

### Flux d'authentification (SSO en 1 echange)

```
Client                                    Serveur
  |                                           |
  | POST /api/auth/login                      |
  | { email, nonce, timestamp, hmac }         |
  | ---------------------------------------->|
  |                                           | 1. Email existe ?
  |                                           | 2. Timestamp dans +/-60s ?
  |                                           | 3. Nonce deja vu ? (anti-rejeu)
  |                                           | 4. HMAC valide ? (temps constant)
  |                                           | 5. Emet JWT
  | { accessToken, expiresAt }               |
  | <----------------------------------------|
```

### Calcul HMAC cote client

```
message = email + ":" + nonce + ":" + timestamp
hmac    = HMAC_SHA256(key=password, data=message)
```

---

## Master Key

### Principe

La Master Key (APP_MASTER_KEY) est utilisee pour chiffrer les mots de passe avant stockage en base.
Elle ne doit jamais etre dans le code source ni dans les fichiers de configuration commites.

### Format de stockage

```
v1:Base64(iv):Base64(ciphertext)
```

Exemple :
```
v1:aGVsbG8=:d2Fzc3VwXzEyMw==
```

### Demarrage

L'application refuse de demarrer si APP_MASTER_KEY est absente ou trop courte (< 32 caracteres).

### Lancer en local

```bash
# Windows
set APP_MASTER_KEY=votre_cle_secrete_de_32_caracteres_min
mvn spring-boot:run

# Linux/Mac
export APP_MASTER_KEY=votre_cle_secrete_de_32_caracteres_min
mvn spring-boot:run
```

---

## Stack technique

- **Java 21** + **Spring Boot 3.2**
- **H2** (base en memoire)
- **JJWT 0.12.3** pour les JWT
- **AES-128/GCM** pour le chiffrement reversible des mots de passe
- **JavaFX 21** pour l'interface utilisateur

---

## Demarrage rapide

### 1. Lancer le serveur

```bash
set APP_MASTER_KEY=0123456789abcdef0123456789abcdef
mvn spring-boot:run
```

Un utilisateur de test est cree automatiquement :
- Email : `alice@example.com`
- Password : `password123`

Console H2 : http://localhost:8080/h2-console
JDBC URL : `jdbc:h2:mem:authdb`

### 2. Lancer l'interface JavaFX

Dans le panneau Maven d'IntelliJ :
```
Plugins -> javafx -> javafx:run
```

---

## API

### Inscription
```http
POST /api/users/register
Content-Type: application/json

{ "email": "user@example.com", "password": "monmotdepasse" }
```

### Login (authentification forte)
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "nonce": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1711234567,
  "hmac": "a3f1c2..."
}
```

### Profil connecte
```http
GET /api/me
Authorization: Bearer eyJhbGci...
```

---

## CI/CD GitHub Actions

La pipeline se declenche automatiquement sur chaque push et pull request vers `main`.

Elle effectue :
1. Checkout du code
2. Installation JDK 21
3. Build Maven
4. Execution des tests JUnit (40+ tests)
5. Analyse SonarCloud
6. Echec automatique si un test echoue
7. Echec automatique si le Quality Gate SonarCloud echoue

### Secrets GitHub requis

| Secret | Description |
|---|---|
| `SONAR_TOKEN` | Token d'authentification SonarCloud |

### Variable d'environnement CI

```yaml
APP_MASTER_KEY: test_master_key_for_ci_only_32chars
```

---

## Lancer les tests

```bash
set APP_MASTER_KEY=test_master_key_for_ci_only_32chars
mvn test
```

Rapport de couverture JaCoCo :
```
target/site/jacoco/index.html
```

---

## Tags Git

| Tag | Description |
|---|---|
| `v4.0-start` | Structure de base TP4 |
| `v4.1-master-key` | Chiffrement AES-GCM + Master Key |
| `v4.2-ci` | GitHub Actions CI/CD |
| `v4.3-tests` | Tests Master Key |
| `v4-tp4` | Tag final |

---

## Avantages du protocole

- Aucun mot de passe (clair ou hache) ne circule sur le reseau
- Le timestamp limite la fenetre d'attaque a **60 secondes**
- Le nonce empeche la **reutilisation** d'une requete interceptee
- La comparaison en **temps constant** previent les timing attacks
- La Master Key n'est jamais dans le code source
- **Sans stockage du nonce en base, le nonce ne sert a rien contre le rejeu**