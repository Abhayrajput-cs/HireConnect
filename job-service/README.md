# Job Service

`job-service` manages the full lifecycle of HireConnect job postings.

## Features

- Job creation, retrieval, update, and deletion
- Search and filter by title, category, location, salary range, experience, status, and recruiter owner
- Recruiter ownership validation through `profile-service`
- MySQL runtime configuration and H2-backed integration tests
- Eureka client registration and actuator health endpoint
- Versioned REST API under `/api/v1/jobs`

## Run Locally

Set environment variables:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/hireconnect_job?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your-password'
$env:EUREKA_SERVER_URL='http://localhost:8761/eureka/'
$env:PROFILE_SERVICE_BASE_URL='http://localhost:8084'
```

Start the service:

```powershell
cd job-service
mvn spring-boot:run
```

## Main Endpoints

- `POST /api/v1/jobs`
- `GET /api/v1/jobs`
- `GET /api/v1/jobs/{jobId}`
- `GET /api/v1/jobs/title/{title}`
- `GET /api/v1/jobs/category/{category}`
- `GET /api/v1/jobs/location/{location}`
- `GET /api/v1/jobs/status/{status}`
- `GET /api/v1/jobs/recruiter/{postedBy}`
- `PUT /api/v1/jobs/{jobId}`
- `DELETE /api/v1/jobs/{jobId}`
- `GET /actuator/health`

## Example Create Request

```json
{
  "title": "Senior Java Developer",
  "category": "Engineering",
  "type": "Full-time",
  "location": "Pune",
  "salaryMin": 1200000,
  "salaryMax": 1800000,
  "description": "Build and scale backend services.",
  "skills": ["Java", "Spring Boot", "MySQL"],
  "experienceRequired": 4,
  "postedBy": 101,
  "status": "OPEN",
  "postedAt": "2026-04-21"
}
```
