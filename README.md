# TP3 — Serveur d'Authentification Forte

## Principe du protocole

Ce serveur implémente une authentification forte par preuve HMAC.  
**Le mot de passe ne circule jamais sur le réseau** (ni en clair, ni haché).

### Flux d'authentification (SSO en 1 échange)

```
Client                                    Serveur
  |                                           |
  | POST /api/auth/login                      |
  | { email, nonce, timestamp, hmac }         |
  | ---------------------------------------->|
  |                                           | 1. Email existe ?
  |                                           | 2. Timestamp dans ±60s ?
  |                                           | 3. Nonce déjà vu ? (anti-rejeu)
  |                                           | 4. HMAC valide ? (temps constant)
  |                                           | 5. Émet JWT
  | { accessToken, expiresAt }               |
  | <----------------------------------------|
```

### Calcul HMAC côté client

```
message = email + ":" + nonce + ":" + timestamp
hmac    = HMAC_SHA256(key=password, data=message)
```

---

## Stack technique

- **Java 17** + **Spring Boot 3.2**
- **H2** (base en mémoire)
- **JJWT 0.12.3** pour les JWT
- **AES-128/CBC** pour le chiffrement réversible des mots de passe

---

## Démarrage rapide

```bash
mvn spring-boot:run
```

Un utilisateur de test est créé automatiquement :
- Email : `alice@example.com`
- Password : `password123`

Console H2 : http://localhost:8080/h2-console  
JDBC URL : `jdbc:h2:mem:authdb`

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

**Réponse 200 :**
```json
{
  "accessToken": "eyJhbGci...",
  "expiresAt": 1711235467
}
```

### Profil connecté
```http
GET /api/me
Authorization: Bearer eyJhbGci...
```

---

## Lancer les tests

```bash
mvn test
```

Rapport de couverture JaCoCo :
```
target/site/jacoco/index.html
```

---

## Tags Git

| Tag | Description |
|-----|-------------|
| `v3.0-start` | Structure de base |
| `v3.1-db-nonce` | Entités + table auth_nonce |
| `v3.2-hmac-client` | Service HMAC |
| `v3.3-hmac-server` | Vérification HMAC serveur |
| `v3.4-anti-replay` | Protection anti-rejeu |
| `v3.5-token` | Émission JWT |
| `v3.6-tests-80` | Tests + couverture 80% |
| `v3-tp3` | Tag final |

---

## ⚠️ Note pédagogique importante

Ce mécanisme est **pédagogique**.

- Le serveur stocke les mots de passe avec un **chiffrement réversible (AES)**.
- Cela est nécessaire pour recalculer le HMAC côté serveur.
- **En industrie**, on évite le chiffrement réversible. On préférerait :
  - Un hash non réversible et adaptatif (argon2id, bcrypt)
  - Un protocole de type **SRP** (Secure Remote Password)
- **Risque** : si la Server Master Key (SMK) est compromise, tous les mots de passe stockés peuvent être déchiffrés.

### Avantages du protocole implémenté

- Aucun mot de passe (clair ou haché) ne circule sur le réseau
- Le timestamp limite la fenêtre d'attaque à 60 secondes
- Le nonce empêche la réutilisation d'une requête interceptée
- La comparaison en temps constant prévient les timing attacks
- **Sans stockage du nonce en base, le nonce ne sert à rien contre le rejeu**
