package com.toollix.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Comma-separated list of allowed origins injected from application.yml.
     * Defaults to both local dev ports so the Next.js frontend works out of the
     * box.
     * In production, set: security.cors.allowed-origins=https://cards.toollix.app
     */
    @Value("${security.cors.allowed-origins:http://localhost:3000,http://localhost:3001,https://cards.toollix.com}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Explicitly list allowed frontend origins from config
        config.setAllowedOrigins(allowedOrigins);
        // Allow standard REST verbs + the preflight OPTIONS
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Allow the Authorization header (JWT) + Content-Type for JSON/multipart
        // requests
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        // Allow browser to read the Location header on 3xx redirects (used by NFC tap
        // flow)
        config.setExposedHeaders(List.of("Location", "Content-Disposition"));
        // Allow cookies / credentials to be sent (needed for httpOnly refresh-token
        // cookie)
        config.setAllowCredentials(true);
        // Cache preflight result for 1 hour to avoid per-request OPTIONS round-trips
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider);

        http
                .csrf(csrf -> csrf.disable())
                // Wire up the CORS bean defined above — this MUST come before any auth rules
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/plans", "/plans/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/tap/**").permitAll()
                        .requestMatchers("/p/**").permitAll() // public profile pages + analytics events
                        .requestMatchers("/uploads/**").permitAll() // static file serving
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/orgs/public/**").permitAll() // org
                                                                                                                 // branded
                                                                                                                 // public
                                                                                                                 // profiles
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**")
                        .permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
