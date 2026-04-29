# HireConnect Notification Service

Merged notification and analytics microservice for HireConnect.

## Purpose

- Consumes asynchronous business events from RabbitMQ
- Stores in-app notifications with read and unread state
- Sends optional email alerts for supported events
- Exposes recruiter and platform analytics endpoints
- Tracks job view counts and application pipeline metrics

## Covered Domains

- Notification operations
  - list notifications by user
  - mark one as read
  - mark all as read
  - delete a notification
  - unread count
- Analytics operations
  - job view count
  - application count by job
  - view-to-apply ratio
  - recruiter pipeline summary
  - platform summary
  - top job categories
  - average time-to-hire

## Endpoints

- `/api/v1/notifications/events`
- `/api/v1/notifications/user/{userId}`
- `/api/v1/notifications/user/{userId}/unread-count`
- `/api/v1/notifications/{notificationId}/read`
- `/api/v1/notifications/user/{userId}/read-all`
- `/api/v1/notifications/{notificationId}`
- `/api/v1/analytics/jobs/{jobId}/views`
- `/api/v1/analytics/jobs/{jobId}/view-count`
- `/api/v1/analytics/jobs/{jobId}/application-count`
- `/api/v1/analytics/jobs/{jobId}/view-to-apply-ratio`
- `/api/v1/analytics/recruiter/{recruiterId}`
- `/api/v1/analytics/recruiter/{recruiterId}/time-to-hire`
- `/api/v1/analytics/admin`
- `/api/v1/analytics/categories/top`

## Messaging

- exchange: `hireconnect.events`
- queue: `hireconnect.notification.queue`
- binding key pattern: `hireconnect.notification.#`

Publishing services:

- `job-service`
- `application-service`
- `interview-service`

## Run

```bash
mvn spring-boot:run
```

Default URLs:

- Service base URL: `http://localhost:8088`
- Health: `http://localhost:8088/actuator/health`

## Local Dependencies

- MySQL database: `hireconnect_notification`
- RabbitMQ on `localhost:5672`

## Test

```bash
mvn test
```
