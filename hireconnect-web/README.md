# HireConnect Web MVC Service

Final MVC website-controller layer for HireConnect.

## Purpose

- integrates backend microservices through REST calls
- provides candidate, recruiter, and admin MVC controllers
- persists local MVC-only features such as bookmarks, wallet balances, subscription plans, invoices, and user suspension records

## URLs

- direct base URL: `http://localhost:8090`
- health: `http://localhost:8090/actuator/health`
- gateway entry: `http://localhost:8080/web/...`

## Main MVC Areas

- candidate: `/candidate/profile`, `/candidate/jobs`, `/candidate/applications`, `/candidate/interviews`, `/candidate/notifications`
- recruiter: `/recruiter/dashboard`, `/recruiter/jobs/new`, `/recruiter/analytics`, `/recruiter/subscription`, `/recruiter/invoices`
- admin: `/admin/dashboard`, `/admin/users`, `/admin/jobs`, `/admin/analytics`, `/admin/subscriptions`, `/admin/invoices`

## Run

```bash
mvn spring-boot:run
```
