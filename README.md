nfc-backend — production-style Spring Boot monolith

Modules: common, auth, users, profiles, nfc, analytics, orders, subscriptions, organizations, admin, app

Quick start:
- Configure environment variables or use Docker Compose for local development
- Required env: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, SECURITY_JWT_SECRET
- Optional: REDIS_HOST, REDIS_PORT, MAIL_ENABLED, MAIL_FROM
- Run:
  - docker-compose up --build
  - Open: http://localhost:8080

Production profile:
- Use `SPRING_PROFILES_ACTIVE=prod` for production-oriented config
- Ensure DB, Redis, and JWT secrets are set via environment or secret manager
- Keep `SECURITY_JWT_SECRET` unique and strong in all environments

This backend includes JWT auth, refresh-token persistence, password hashing, verification flow, audit logging, entitlement checks, profile/org/NFC domain models, Redis-backed rate limiting, Docker compose setup, and deployment manifests.

Production checklist:
- Rotate SECURITY_JWT_SECRET and credentials via secret manager
- Use real SMTP mail provider for verification emails
- Keep PostgreSQL and Redis in managed infra
- Add ingress, TLS, and WAF in front of the app
- Use Kubernetes or container orchestration for scale and updates

Current status:
- Multi-module build is working and validated
- Core auth, profile, org, subscription, and NFC flows are in place
- Production deployment manifests are prepared in `k8s/`
