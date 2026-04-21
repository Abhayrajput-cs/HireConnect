package com.hireconnect.gateway.config;

import java.util.function.Function;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", serviceRoute("/api/auth/**", "/api/auth/(?<segment>.*)", "/auth/${segment}", "lb://auth-service"))
            .route("profile-service", predicate -> predicate
                .path("/api/v1/profiles", "/api/v1/profiles/**")
                .uri("lb://profile-service"))
            .route("job-service-v1", predicate -> predicate
                .path("/api/v1/jobs", "/api/v1/jobs/**")
                .uri("lb://job-service"))
            .route("job-service-legacy", serviceRoute("/api/jobs/**", "/api/jobs/(?<segment>.*)", "/api/v1/jobs/${segment}", "lb://job-service"))
            .route("application-service", serviceRoute("/api/applications/**", "/api/applications/(?<segment>.*)", "/applications/${segment}", "lb://application-service"))
            .route("interview-service", serviceRoute("/api/interviews/**", "/api/interviews/(?<segment>.*)", "/interviews/${segment}", "lb://interview-service"))
            .route("notification-service", serviceRoute("/api/notifications/**", "/api/notifications/(?<segment>.*)", "/notifications/${segment}", "lb://notification-service"))
            .route("subscription-service", serviceRoute("/api/subscriptions/**", "/api/subscriptions/(?<segment>.*)", "/subscriptions/${segment}", "lb://subscription-service"))
            .route("analytics-service", serviceRoute("/api/analytics/**", "/api/analytics/(?<segment>.*)", "/analytics/${segment}", "lb://analytics-service"))
            .route("hireconnect-web", serviceRoute("/web/**", "/web/(?<segment>.*)", "/${segment}", "lb://hireconnect-web"))
            .build();
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
