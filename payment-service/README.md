# HireConnect Payment Service

Spring Boot microservice for Razorpay-backed subscriptions for HireConnect candidates and recruiters.

## Folder Structure

```text
payment-service/
  pom.xml
  src/main/resources/application.properties
  src/main/java/com/hireconnect/payment/
    PaymentServiceApplication.java
    config/
      RazorpayProperties.java
      JwtProperties.java
      SecurityConfig.java
      SubscriptionPlanInitializer.java
    controller/
      PaymentResource.java
    domain/
      PaymentStatus.java
      PaymentTransaction.java
      PlanType.java
      SubscriptionPlan.java
      SubscriptionStatus.java
      UserRole.java
      UserSubscription.java
    dto/
      ApiResponse.java
      CreateOrderRequest.java
      CreateOrderResponse.java
      PaymentTransactionResponse.java
      SubscriptionPlanResponse.java
      SubscriptionStatusResponse.java
      UserSubscriptionResponse.java
      VerifyPaymentRequest.java
    exception/
      ApiException.java
      GlobalExceptionHandler.java
    repository/
      PaymentTransactionRepository.java
      SubscriptionPlanRepository.java
      UserSubscriptionRepository.java
    security/
      AuthenticatedUser.java
      AuthValidationClient.java
      JwtAuthenticationFilter.java
      JwtService.java
    service/
      RazorpayClient.java
      PaymentService.java
      PaymentServiceImpl.java
```

## Plans Seeded on Startup

- `CANDIDATE_FREE`
- `CANDIDATE_PREMIUM`: featured profile, priority applications, application analytics, resume/profile visibility boost
- `RECRUITER_FREE`
- `RECRUITER_PREMIUM`: more job posts, premium candidate profiles, direct contact, advanced filters/search, analytics dashboard access

## Environment

```properties
SERVER_PORT=8089
DB_URL=jdbc:mysql://localhost:3306/hireconnect_payment?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_password
EUREKA_SERVER_URL=http://localhost:8761/eureka/
AUTH_SERVICE_BASE_URL=http://localhost:8081
JWT_SECRET=...
RAZORPAY_BASE_URL=https://api.razorpay.com
RAZORPAY_KEY_ID=rzp_test_your_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
RAZORPAY_MOCK_MODE=false
```

Use Razorpay test-mode keys for local/demo payments. Keep `RAZORPAY_MOCK_MODE=false` when you want the real Razorpay checkout window to open.

## Postman Testing Flow

Use the API Gateway base URL:

```text
http://localhost:8080
```

1. Login through auth-service and copy the access token.

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "candidate@example.com",
  "password": "Secure123!"
}
```

2. Fetch candidate plans.

```http
GET /api/v1/payments/plans/CANDIDATE
```

3. Create premium order.

```http
POST /api/v1/payments/create-order
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "userId": 7,
  "role": "CANDIDATE",
  "planType": "CANDIDATE_PREMIUM",
  "customerName": "Test Candidate",
  "customerEmail": "candidate@example.com",
  "customerPhone": "9876543210"
}
```

4. Open Razorpay Checkout from the frontend using `gatewayOrderId`, `razorpayKeyId`, and `amountInPaise` returned by the create-order response.

5. Verify payment after Razorpay success. The frontend sends these fields automatically from Razorpay Checkout.

```http
POST /api/v1/payments/verify
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "orderId": "{{orderId}}",
  "transactionId": "{{razorpay_payment_id}}",
  "razorpayOrderId": "{{razorpay_order_id}}",
  "razorpayPaymentId": "{{razorpay_payment_id}}",
  "razorpaySignature": "{{razorpay_signature}}"
}
```

6. Check premium status from any service/frontend.

```http
GET /api/v1/payments/subscription/status/7
```

7. Fetch subscription details.

```http
GET /api/v1/payments/subscription/7
Authorization: Bearer {{accessToken}}
```

8. Cancel subscription.

```http
POST /api/v1/payments/cancel/7
Authorization: Bearer {{accessToken}}
```

Recruiter flow is the same, using `RECRUITER` and `RECRUITER_PREMIUM`.
