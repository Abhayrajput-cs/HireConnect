# HireConnect Interview Service

Handles interview scheduling and lifecycle management for job applications.

## Features

- recruiter interview scheduling with online or in-person mode
- candidate interview confirmation and reschedule requests
- recruiter-driven rescheduling and cancellation
- lookup by interview id, application id, status, and scheduled date range
- cross-service validation against `application-service`, `job-service`, and `profile-service`
- Eureka registration and actuator health checks

## Endpoints

- `POST /api/v1/interviews`
- `GET /api/v1/interviews/{interviewId}`
- `GET /api/v1/interviews/application/{applicationId}`
- `GET /api/v1/interviews/status/{status}`
- `GET /api/v1/interviews?scheduledFrom=2026-04-22T09:00:00&scheduledTo=2026-04-22T18:00:00`
- `PATCH /api/v1/interviews/{interviewId}/confirm`
- `PATCH /api/v1/interviews/{interviewId}/reschedule`
- `DELETE /api/v1/interviews/{interviewId}`

## Run

```bash
mvn spring-boot:run
```

Default URLs:

- service: `http://localhost:8087`
- health: `http://localhost:8087/actuator/health`

## Configuration

Main settings live in `src/main/resources/application.properties`.

- `DB_URL` defaults to `hireconnect_interview`
- `SERVER_PORT` defaults to `8087`
- `EUREKA_SERVER_URL` defaults to `http://localhost:8761/eureka/`
- `AUTH_SERVICE_BASE_URL` defaults to `http://localhost:8081`
- `PROFILE_SERVICE_BASE_URL` defaults to `http://localhost:8084`
- `JOB_SERVICE_BASE_URL` defaults to `http://localhost:8085`
- `APPLICATION_SERVICE_BASE_URL` defaults to `http://localhost:8086`

## Security

- all interview endpoints require login
- recruiters can schedule, reschedule, cancel, and view status/date-range interview listings
- candidates can confirm and request rescheduling for their own interviews

## Test

```bash
mvn test
```
