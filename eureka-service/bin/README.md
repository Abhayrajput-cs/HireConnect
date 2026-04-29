# HireConnect Eureka Service

Service discovery server for the HireConnect microservice system.

## Purpose

- Central registry for all HireConnect backend services
- Lets services register themselves and discover each other
- Prepares the platform for the upcoming API gateway and remaining microservices

## Stack

- Spring Boot `4.0.5`
- Spring Cloud `2025.1.0`
- Netflix Eureka Server
- Java `21`

## Run

```bash
mvn spring-boot:run
```

Default URLs:

- Dashboard: `http://localhost:8761`
- Registry endpoint: `http://localhost:8761/eureka`
- Health: `http://localhost:8761/actuator/health`

## Configuration

Main settings are in `src/main/resources/application.properties`.

- `SERVER_PORT` defaults to `8761`
- `EUREKA_HOSTNAME` defaults to `localhost`

## Test

```bash
mvn test
```

The test suite verifies that the Eureka service boots and exposes the actuator health endpoint successfully.
