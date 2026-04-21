# HireConnect Application Service

Handles job application submission and lifecycle management.

## Features

- candidate application submission
- application lookup by id, candidate, job, status, and applied date range
- recruiter status progression through the hiring pipeline
- candidate withdrawal support
- duplicate application prevention
- cross-service validation against `profile-service` and `job-service`
- Eureka registration and actuator health checks

## Endpoints

- `POST /api/v1/applications`
- `GET /api/v1/applications/{applicationId}`
- `GET /api/v1/applications/candidate/{candidateId}`
- `GET /api/v1/applications/job/{jobId}`
- `GET /api/v1/applications/job/{jobId}/count`
- `GET /api/v1/applications?status=SHORTLISTED`
- `GET /api/v1/applications?appliedFrom=2026-04-21&appliedTo=2026-04-21`
- `PATCH /api/v1/applications/{applicationId}/status`
- `PATCH /api/v1/applications/{applicationId}/withdraw`

## Run

```bash
mvn spring-boot:run
```

Default URLs:

- service: `http://localhost:8086`
- health: `http://localhost:8086/actuator/health`

## Configuration

Main settings live in `src/main/resources/application.properties`.

- `DB_URL` defaults to `hireconnect_application`
- `SERVER_PORT` defaults to `8086`
- `EUREKA_SERVER_URL` defaults to `http://localhost:8761/eureka/`
- `PROFILE_SERVICE_BASE_URL` defaults to `http://localhost:8084`
- `JOB_SERVICE_BASE_URL` defaults to `http://localhost:8085`
- `AUTH_SERVICE_BASE_URL` defaults to `http://localhost:8081`

## Security

- all application endpoints require login
- candidates can submit and withdraw their own applications
- recruiters can review job-level applications and update statuses

## Test

```bash
mvn test
```
