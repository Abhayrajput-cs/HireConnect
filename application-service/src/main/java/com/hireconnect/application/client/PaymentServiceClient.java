package com.hireconnect.application.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.hireconnect.application.exception.ApiException;

@Component
public class PaymentServiceClient implements SubscriptionAccessClient {

    private final RestClient restClient;

    public PaymentServiceClient(@Value("${app.services.payment.base-url}") String paymentServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(paymentServiceBaseUrl).build();
    }

    @Override
    public boolean hasPremiumAccess(Integer userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        try {
            PaymentApiResponse<SubscriptionStatusSnapshot> response = restClient.get()
                .uri("/api/v1/payments/subscription/status/{userId}", userId.longValue())
                .retrieve()
                .body(new ParameterizedTypeReference<PaymentApiResponse<SubscriptionStatusSnapshot>>() {
                });
            return response != null && response.data() != null && response.data().premiumActive();
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payment service is unavailable for subscription validation");
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment service returned an unexpected subscription response");
        }
    }
}
