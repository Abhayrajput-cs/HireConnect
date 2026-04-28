package com.hireconnect.interview.config;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import com.hireconnect.interview.security.JwtAuthenticationFilter;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @org.springframework.context.annotation.Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @org.springframework.beans.factory.annotation.Value("${app.security.enabled:true}") boolean securityEnabled
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/interviews").hasRole("RECRUITER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/interviews/*/confirm").hasRole("CANDIDATE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/interviews/*/reschedule").hasAnyRole("CANDIDATE", "RECRUITER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/interviews/*").hasRole("RECRUITER")
                .requestMatchers(HttpMethod.GET, "/api/v1/interviews/status/**").hasRole("RECRUITER")
                .requestMatchers(HttpMethod.GET, "/api/v1/interviews").hasRole("RECRUITER")
                .requestMatchers("/api/v1/interviews/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
