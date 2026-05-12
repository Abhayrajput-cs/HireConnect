# Profile Service

`profile-service` manages candidate and recruiter profiles for HireConnect.

## Features

- Candidate profile creation with skills, experience, resume URL, and shared addresses
- Recruiter profile creation with company details and shared addresses
- Lookup by profile id, email, mobile, and role
- Update and delete profile operations
- MySQL runtime configuration and H2-backed integration tests
- Eureka client registration and actuator health endpoint
- Versioned REST API under `/api/v1/profiles`

## Run Locally

Set environment variables:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/hireconnect_profile?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your-password'
$env:EUREKA_SERVER_URL='http://localhost:8761/eureka/'
```

Start the service:

```powershell
cd profile-service
mvn spring-boot:run
```

## Main Endpoints

- `POST /api/v1/profiles/candidates`
- `POST /api/v1/profiles/recruiters`
- `GET /api/v1/profiles`
- `GET /api/v1/profiles/{profileId}`
- `GET /api/v1/profiles/email/{email}`
- `GET /api/v1/profiles/mobile/{mobile}`
- `GET /api/v1/profiles/role/{role}`
- `PUT /api/v1/profiles/{profileId}`
- `DELETE /api/v1/profiles/{profileId}`
- `GET /actuator/health`

## Example Candidate Create Request

```json
{
  "fullName": "Aman Sharma",
  "email": "aman@example.com",
  "mobile": 9876543210,
  "dob": "1999-03-15",
  "gender": "MALE",
  "skills": ["Java", "Spring Boot", "MySQL"],
  "experience": 3,
  "resumeUrl": "https://files.example.com/resume/aman.pdf",
  "addresses": [
    {
      "houseNo": "12A",
      "street": "MG Road",
      "city": "Pune",
      "state": "Maharashtra",
      "pincode": 411001
    }
  ]
}
```

## Example Recruiter Create Request

```json
{
  "fullName": "Priya Verma",
  "email": "priya@hireco.com",
  "mobile": 9988776655,
  "companyName": "HireCo",
  "companySize": "201-500",
  "industry": "Software",
  "website": "https://hireco.example.com",
  "addresses": [
    {
      "houseNo": "9",
      "street": "Cyber City",
      "city": "Gurgaon",
      "state": "Haryana",
      "pincode": 122002
    }
  ]
}
```
