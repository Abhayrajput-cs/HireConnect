package com.hireconnect.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class ApiGatewayServiceApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void routeTableContainsCoreMicroserviceEntries() {
        List<String> routeIds = routeLocator.getRoutes()
            .map(Route::getId)
            .collectList()
            .block();

        assertThat(routeIds).contains(
            "auth-service",
            "profile-service",
            "job-service-v1",
            "job-service-legacy",
            "application-service-v1",
            "application-service-legacy",
            "interview-service-v1",
            "interview-service-legacy",
            "notification-service-v1",
            "notification-service-legacy",
            "analytics-service-v1",
            "analytics-service-legacy",
            "hireconnect-web-root",
            "hireconnect-web"
        );
    }
}
