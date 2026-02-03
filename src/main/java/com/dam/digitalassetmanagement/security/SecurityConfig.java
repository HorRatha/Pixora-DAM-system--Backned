package com.dam.digitalassetmanagement.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - Authentication
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login",
                                "/api/auth/password/forgot",      // ✅ Password reset flow
                                "/api/auth/password/verify-otp",  // ✅ OTP verification
                                "/api/auth/password/reset"        // ✅ Final password reset
                        ).permitAll()

                        // Swagger/OpenAPI documentation
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                        // Health check endpoint
                        .requestMatchers("/actuator/health").permitAll()

                        // ✅ CRITICAL: Profile picture endpoints - MUST BE BEFORE /api/users/**
                        .requestMatchers(HttpMethod.GET, "/api/users/profile-picture/**").permitAll() // Public access to view profile pictures
                        .requestMatchers(HttpMethod.POST, "/api/users/me/profile-picture").authenticated() // Upload requires auth
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me/profile-picture").authenticated() // Delete requires auth

                        // ✅ User profile endpoints - MUST BE BEFORE generic /api/users/**
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()

                        // ============================================
                        // ✅ ADDED: PUBLIC ASSET DETAIL ENDPOINT
                        // ============================================
                        // Asset detail view (public - anyone can view)
                        .requestMatchers(HttpMethod.GET, "/api/assets/{id}").permitAll()

                        // ============================================
                        // ✅ ADDED: SOCIAL FEATURES - PUBLIC ENDPOINTS
                        // ============================================

                        // Views (Recording and counting views)
                        .requestMatchers(HttpMethod.POST, "/api/assets/*/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/assets/*/views/count").permitAll()

                        // Reactions (Likes - counting and checking)
                        .requestMatchers(HttpMethod.GET, "/api/reactions/asset/*/count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reactions/asset/*/has-reacted").permitAll()

                        // Comments (Reading and counting)
                        .requestMatchers(HttpMethod.GET, "/api/comments/asset/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/asset/*/count").permitAll()

                        // WebSocket for real-time updates
                        .requestMatchers("/ws/**").permitAll()

                        // ============================================
                        // ✅ SOCIAL FEATURES - AUTHENTICATED ENDPOINTS
                        // ============================================

                        // Reactions (Creating/toggling requires auth)
                        .requestMatchers(HttpMethod.POST, "/api/reactions").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reactions/*").authenticated()

                        // Comments (Creating/deleting requires auth)
                        .requestMatchers(HttpMethod.POST, "/api/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*").authenticated()

                        // Public asset endpoints
                        .requestMatchers("/api/assets/public/**").permitAll()

                        // ✅ FIXED: Asset endpoints - MORE SPECIFIC RULES FIRST
                        .requestMatchers(HttpMethod.GET, "/api/assets").permitAll() // List all assets - public
                        .requestMatchers(HttpMethod.GET, "/api/assets/my-assets").authenticated() // My assets - auth required
                        .requestMatchers(HttpMethod.POST, "/api/assets").hasAnyRole("UPLOADER", "EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/assets/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/assets/**").authenticated()

                        // Collection endpoints
                        .requestMatchers(HttpMethod.GET, "/api/collections/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/collections/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/collections/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/collections/**").authenticated()

                        // Search endpoints
                        .requestMatchers("/api/search/**").authenticated()

                        // Admin-only endpoints - LAST, after specific /me and profile-picture endpoints
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit-logs/**").hasAnyRole("EDITOR", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}