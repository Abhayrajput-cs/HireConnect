# HireConnect Auth Service

Spring Boot auth microservice for the HireConnect case study.

## Features

- Local registration and login for `CANDIDATE` and `RECRUITER`
- GitHub OAuth2 login with role selection via `/oauth2/authorization/github?role=RECRUITER`
- JWT access tokens, refresh tokens, token validation, and logout endpoint
- BCrypt password hashing
- Single-table auth model with only the `users` table
- Role-aware security and protected `/auth/me`
- Actuator health endpoint at `/actuator/health`
- MySQL local setup with an H2-backed test profile

## API

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/validate`
- `GET /auth/me`
- `GET /oauth2/authorization/github?role=CANDIDATE&redirect_uri=http://localhost:3000/oauth2/callback`

## Run

```bash
mvn spring-boot:run
```

## Test

```bash
mvn test
```

## Postman

Import these files into Postman:

- `postman/HireConnect Auth.postman_collection.json`
- `postman/HireConnect Auth.local.postman_environment.json`

Run the requests in this order:

1. `Health`
2. `Register`
3. `Login`
4. `Validate Token`
5. `Me`
6. `Refresh Token`
7. `Logout`
8. `Validate After Logout`

GitHub OAuth still needs a browser-based flow, so the local Postman collection focuses on the REST auth endpoints.

## Database Structure

The auth service now follows the case-study structure with a single `users` table containing only:

- `user_id`
- `email`
- `password_hash`
- `role`
- `provider`
- `created_at`

Because the database is restricted to that single table, logout is stateless. The endpoint exists and the client should discard its tokens after calling it.

## Important Environment Variables

- `JWT_SECRET`
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OAUTH2_DEFAULT_SUCCESS_URL`

`application.properties` is now safe to commit and expects secrets from environment variables.

For deployment, start from:

- `.env.production.example`

Your current production-style values have been saved locally in an ignored file:

- `.secrets/auth-service-production.env`

That file is not meant to be committed. Move those values into your server environment or secret manager before production deployment.
