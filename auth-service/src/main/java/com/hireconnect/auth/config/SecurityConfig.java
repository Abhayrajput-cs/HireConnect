package com.hireconnect.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.hireconnect.auth.security.JwtAuthenticationFilter;
import com.hireconnect.auth.security.OAuth2AuthenticationFailureHandler;
import com.hireconnect.auth.security.OAuth2AuthenticationSuccessHandler;
import com.hireconnect.auth.security.OAuth2AuthorizationRequestFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        OAuth2AuthorizationRequestFilter oAuth2AuthorizationRequestFilter,
        OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
        OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                PathPatternRequestMatcher.withDefaults().matcher("/auth/**")
            ))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/h2-console/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/auth/register",
                    "/auth/login",
                    "/auth/refresh",
                    "/auth/validate",
                    "/auth/logout"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );

        http.addFilterBefore(oAuth2AuthorizationRequestFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
