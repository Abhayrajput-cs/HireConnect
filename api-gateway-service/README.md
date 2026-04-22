# HireConnect API Gateway Service

Central entry point for client requests in the HireConnect microservices platform.

## Purpose

- Accepts client traffic through a single gateway URL
- Routes requests to backend services through Eureka service discovery
- Hides direct service URLs from frontend and external clients
- Prepares the system for cross-cutting concerns such as auth propagation, rate limiting, and centralized filters

## Stack

- Spring Boot `4.0.5`
- Spring Cloud `2025.1.0`
- Spring Cloud Gateway Server WebFlux
- Netflix Eureka Client
- Java `21`

## Routes

The gateway currently defines friendly entry routes for the planned microservices:

- `/api/auth/**` -> `auth-service`
- `/api/v1/profiles/**` -> `profile-service`
- `/api/v1/jobs/**` -> `job-service`
- `/api/jobs/**` -> `job-service` legacy alias to `/api/v1/jobs/**`
- `/api/v1/applications/**` -> `application-service`
- `/api/applications/**` -> `application-service` legacy alias to `/api/v1/applications/**`
- `/api/v1/interviews/**` -> `interview-service`
- `/api/interviews/**` -> `interview-service` legacy alias to `/api/v1/interviews/**`
- `/api/notifications/**` -> `notification-service`
- `/api/subscriptions/**` -> `subscription-service`
- `/api/analytics/**` -> `analytics-service`
- `/web/**` -> `hireconnect-web`

## Run

```bash
mvn spring-boot:run
```

Default URLs:

- Gateway base URL: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Gateway actuator: `http://localhost:8080/actuator/gateway`

## Configuration

Main settings are in `src/main/resources/application.properties`.

- `SERVER_PORT` defaults to `8080`
- `EUREKA_SERVER_URL` defaults to `http://localhost:8761/eureka/`

## Test

```bash
mvn test
```

The test verifies that the gateway boots and exposes route definitions for the core HireConnect services.
