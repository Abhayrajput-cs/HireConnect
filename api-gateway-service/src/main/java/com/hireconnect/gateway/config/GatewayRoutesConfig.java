package com.hireconnect.gateway.config;

import java.util.function.Function;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class GatewayRoutesConfig {

    private final String authServiceUri;
    private final String profileServiceUri;
    private final String jobServiceUri;
    private final String applicationServiceUri;
    private final String interviewServiceUri;
    private final String notificationServiceUri;
    private final String paymentServiceUri;
    private final String hireconnectWebUri;

    GatewayRoutesConfig(Environment environment) {
        this.authServiceUri = routeUri(environment, "AUTH_SERVICE_BASE_URL", "lb://auth-service");
        this.profileServiceUri = routeUri(environment, "PROFILE_SERVICE_BASE_URL", "lb://profile-service");
        this.jobServiceUri = routeUri(environment, "JOB_SERVICE_BASE_URL", "lb://job-service");
        this.applicationServiceUri = routeUri(environment, "APPLICATION_SERVICE_BASE_URL", "lb://application-service");
        this.interviewServiceUri = routeUri(environment, "INTERVIEW_SERVICE_BASE_URL", "lb://interview-service");
        this.notificationServiceUri = routeUri(environment, "NOTIFICATION_SERVICE_BASE_URL", "lb://notification-service");
        this.paymentServiceUri = routeUri(environment, "PAYMENT_SERVICE_BASE_URL", "lb://payment-service");
        this.hireconnectWebUri = routeUri(environment, "HIRECONNECT_WEB_BASE_URL", "lb://hireconnect-web");
    }

    @Bean
    RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", serviceRoute("/api/auth/**", "/api/auth/(?<segment>.*)", "/auth/${segment}", authServiceUri))
            .route("profile-service", predicate -> predicate
                .path("/api/v1/profiles", "/api/v1/profiles/**")
                .uri(profileServiceUri))
            .route("job-service-v1", predicate -> predicate
                .path("/api/v1/jobs", "/api/v1/jobs/**")
                .uri(jobServiceUri))
            .route("job-service-legacy", serviceRoute("/api/jobs/**", "/api/jobs/(?<segment>.*)", "/api/v1/jobs/${segment}", jobServiceUri))
            .route("application-service-v1", predicate -> predicate
                .path("/api/v1/applications", "/api/v1/applications/**")
                .uri(applicationServiceUri))
            .route("application-service-legacy", serviceRoute(
                "/api/applications/**",
                "/api/applications/(?<segment>.*)",
                "/api/v1/applications/${segment}",
                applicationServiceUri
            ))
            .route("interview-service-v1", predicate -> predicate
                .path("/api/v1/interviews", "/api/v1/interviews/**")
                .uri(interviewServiceUri))
            .route("interview-service-legacy", serviceRoute(
                "/api/interviews/**",
                "/api/interviews/(?<segment>.*)",
                "/api/v1/interviews/${segment}",
                interviewServiceUri
            ))
            .route("notification-service-v1", predicate -> predicate
                .path("/api/v1/notifications", "/api/v1/notifications/**")
                .uri(notificationServiceUri))
            .route("notification-service-legacy", serviceRoute(
                "/api/notifications/**",
                "/api/notifications/(?<segment>.*)",
                "/api/v1/notifications/${segment}",
                notificationServiceUri
            ))
            .route("analytics-service-v1", predicate -> predicate
                .path("/api/v1/analytics", "/api/v1/analytics/**")
                .uri(notificationServiceUri))
            .route("analytics-service-legacy", serviceRoute(
                "/api/analytics/**",
                "/api/analytics/(?<segment>.*)",
                "/api/v1/analytics/${segment}",
                notificationServiceUri
            ))
            .route("payment-service-v1", predicate -> predicate
                .path("/api/v1/payments", "/api/v1/payments/**")
                .uri(paymentServiceUri))
            .route("payment-service-legacy", serviceRoute(
                "/api/payments/**",
                "/api/payments/(?<segment>.*)",
                "/api/v1/payments/${segment}",
                paymentServiceUri
            ))
            .route("hireconnect-web-root", predicate -> predicate
                .path("/web", "/web/")
                .filters(filter -> filter.rewritePath("/web/?", "/"))
                .uri(hireconnectWebUri))
            .route("hireconnect-web", serviceRoute("/web/**", "/web/(?<segment>.*)", "/${segment}", hireconnectWebUri))
            .build();
    }

    private String routeUri(Environment environment, String key, String fallback) {
        String value = environment.getProperty(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private Function<PredicateSpec, Buildable<Route>> serviceRoute(
        String pathPattern,
        String rewritePattern,
        String replacement,
        String targetUri
    ) {
        return predicate -> predicate.path(pathPattern)
            .filters(filter -> filter.rewritePath(rewritePattern, replacement))
            .uri(targetUri);
    }
}
