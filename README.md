# JWT Refresh Token Authentication

> Stateless JWT authentication with refresh token rotation, theft detection, and secure logout.

---

## Overview

Most developers implement JWT and assume logout is handled by deleting the token from the browser.

It is not.

The token is still valid on the server until it expires. If it was stolen before logout, the attacker still has full access.

This repository demonstrates the correct approach — short-lived access tokens paired with a rotating refresh token system that gives you real session invalidation without sacrificing the stateless benefits of JWT.

---

## How It Works
```
POST /api/auth/login
  ├── Access Token  → 15 min  → returned in response body
  └── Refresh Token → 7 days  → stored in HttpOnly cookie

Every API Request
  └── JWT validated locally — no database hit

Access Token Expires
  └── POST /api/auth/refresh
        ├── Old refresh token revoked
        ├── New refresh token issued (rotation)
        └── New access token returned

POST /api/auth/logout
  └── Refresh token deleted from database
        └── All sessions invalidated
```

---

## Refresh Token Rotation & Theft Detection

Every time a refresh token is used, it is revoked and replaced with a new one.

If a stolen token is reused, the server detects it and immediately invalidates all sessions for that user.
```
Attack Scenario
  Attacker uses stolen refresh_token_A (already revoked)
    └── Server detects reuse of a revoked token
    └── All tokens for this user are wiped
    └── Attacker is locked out
    └── Real user logs in again with clean session
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.12 |
| Security | Spring Security |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Token | jjwt 0.11.5 |
| Utility | Lombok |

---

## Project Structure
```
src/main/java/com/example/auth/
├── config/
│   └── SecurityConfig.java          # Spring Security configuration
├── controller/
│   └── AuthController.java          # REST endpoints
├── dto/
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   └── RegisterRequest.java
├── entity/
│   ├── RefreshToken.java            # Refresh token table mapping
│   └── User.java                    # User table mapping
├── filter/
│   └── JwtAuthFilter.java           # Validates JWT on every request
├── repository/
│   ├── RefreshTokenRepository.java
│   └── UserRepository.java
└── service/
    ├── AuthService.java             # Login, register, refresh, logout
    ├── JwtService.java              # Token generation and validation
    └── RefreshTokenService.java     # Create, rotate, revoke tokens
```

---

## API Reference

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/auth/register` | Register a new user | No |
| `POST` | `/api/auth/login` | Login and receive tokens | No |
| `POST` | `/api/auth/refresh` | Rotate refresh token | Cookie |
| `POST` | `/api/auth/logout` | Invalidate all sessions | Cookie |
| `GET` | `/api/auth/me` | Get authenticated user info | Bearer Token |

---

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL
- Maven

### 1. Clone the repository
```bash
git clone https://github.com/taniy8/jwt-refresh-token-springboot.git
cd jwt-refresh-token-springboot
```

### 2. Set up the database
```bash
psql -U postgres -f sql/schema.sql
```

### 3. Generate a secure JWT secret
```bash
openssl rand -base64 64
```

### 4. Configure application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jwt_auth_db
    username: your_username
    password: your_password

jwt:
  secret: your-generated-secret
  access-expiry-ms: 900000       # 15 minutes
  refresh-expiry-ms: 604800000   # 7 days
```

### 5. Run the application
```bash
mvn spring-boot:run
```

### 6. Test the endpoints

Application starts at `http://localhost:8080`

Use any HTTP client to test the endpoints listed in the API Reference above.

---

## Design Decisions

**Short-lived access token (15 min)**
Limits the damage window if a token is stolen. The attacker's access expires quickly with no action needed.

**Refresh token in HttpOnly cookie**
JavaScript cannot read HttpOnly cookies. This protects the refresh token from XSS attacks entirely.

**Refresh token stored in PostgreSQL**
ACID guarantees ensure that logout invalidation is reliable. No partial deletes, no data loss on crash. Redis is fast but the wrong tool for security-critical state.

**Rotation on every refresh**
A stolen refresh token becomes useless after one use. Attempting to reuse a revoked token is treated as a theft signal and triggers full session invalidation.

**Logout invalidates all sessions**
Deleting all refresh tokens for a user on logout ensures every device is signed out simultaneously.

---

## The Core Insight

The tradeoff is not JWT versus Sessions.

It is short-lived access token versus long-lived access token.

Sessions are not bad. Stateless is not always good. The goal is minimising how often you hit a stateful store, not eliminating state entirely.

---

## Related

- [LinkedIn post that inspired this repo](https://www.linkedin.com/feed/update/urn:li:activity:7442226328529084417/)

---

## License

MIT
