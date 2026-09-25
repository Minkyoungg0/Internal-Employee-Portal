package com.bitcomputer.employeeportal.config;

import com.bitcomputer.employeeportal.auth.AuthController.CurrentUserResponse;
import com.bitcomputer.employeeportal.auth.CurrentAccountStatusFilter;
import com.bitcomputer.employeeportal.auth.EmployeeAccountRepository;
import com.bitcomputer.employeeportal.auth.PortalPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(EmployeeAccountRepository accountRepository) {
        return username -> accountRepository.findWithEmployeeByUsername(username)
                .map(PortalPrincipal::forLogin)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(username));
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CurrentAccountStatusFilter currentAccountStatusFilter,
            ObjectMapper objectMapper
    ) throws Exception {
        return http
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/actuator/health", "/api/auth/csrf", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/me/**", "/api/auth/me", "/api/auth/logout").hasAnyRole("EMPLOYEE", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> {
                            PortalPrincipal principal = (PortalPrincipal) authentication.getPrincipal();
                            writeJson(response, HttpServletResponse.SC_OK, CurrentUserResponse.from(principal), objectMapper);
                        })
                        .failureHandler((request, response, exception) -> writeJson(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("code", "INVALID_CREDENTIALS", "message", "아이디 또는 비밀번호를 확인해 주세요."),
                                objectMapper
                        )))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredSessionStrategy(event -> writeJson(
                                event.getResponse(),
                                HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("code", "SESSION_EXPIRED", "message", "세션이 만료되었습니다."),
                                objectMapper
                        )))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeJson(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                Map.of("code", "UNAUTHENTICATED", "message", "로그인이 필요합니다."),
                                objectMapper
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeJson(
                                response,
                                HttpServletResponse.SC_FORBIDDEN,
                                Map.of("code", "ACCESS_DENIED", "message", "요청할 권한이 없습니다."),
                                objectMapper
                        )))
                .addFilterBefore(currentAccountStatusFilter, AuthorizationFilter.class)
                .httpBasic(basic -> basic.disable())
                .build();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    private static void writeJson(
            HttpServletResponse response,
            int status,
            Object body,
            ObjectMapper objectMapper
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
