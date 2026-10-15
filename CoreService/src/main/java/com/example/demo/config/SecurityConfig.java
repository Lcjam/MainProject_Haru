package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.JwtAuthenticationEntryPoint;
import com.example.demo.exception.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final OAuth2Config oauth2Config;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler((request, response, exception) ->
                                        JwtAuthenticationEntryPoint.writeError(response, ApiErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/core/auth/signup", "/api/core/auth/login",
                                "/api/core/auth/oauth2/exchange", "/api/core/market/products/all/filter")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/core/auth/oauth2/**", "/api/core/hobbies",
                                "/api/core/hobbies/simple", "/api/core/hobbies/categories",
                                "/api/core/hobbies/categories/*", "/api/core/profiles/user/*",
                                "/api/core/profiles/image/**", "/api/core/market/products/all",
                                "/api/core/market/products/{id:[0-9]+}", "/api/core/market/products/images/**",
                                "/ws", "/ws/**")
                        .permitAll()
                        .requestMatchers("/api/core/profiles/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // JWT 필터 추가
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, jwtTokenBlacklistService),
                        UsernamePasswordAuthenticationFilter.class)
                // 로그아웃 비활성화
                .logout(logout -> logout.disable());

        // OAuth2 설정 추가
        oauth2Config.configure(http);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 보안 강도 12 설정
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 등의 Java 8 시간 타입 지원
        return objectMapper;
    }
}
