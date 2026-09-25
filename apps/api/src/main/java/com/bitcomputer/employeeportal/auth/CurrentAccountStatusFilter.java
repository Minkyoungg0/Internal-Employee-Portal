package com.bitcomputer.employeeportal.auth;

import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CurrentAccountStatusFilter extends OncePerRequestFilter {
    private final EmployeeAccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    public CurrentAccountStatusFilter(EmployeeAccountRepository accountRepository, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/health")
                || path.equals("/actuator/health")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/csrf");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PortalPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        EmployeeAccount account = accountRepository.findWithEmployeeById(principal.accountId()).orElse(null);
        if (account == null || !account.isEnabled()
                || account.getEmployee().getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            invalidateSession(request);
            writeUnauthorized(response, "SESSION_REVOKED", "계정 상태가 변경되어 다시 로그인해야 합니다.");
            return;
        }

        if (account.isPasswordChangeRequired() && !passwordChangeAllowed(request.getRequestURI())) {
            writeForbidden(response, "PASSWORD_CHANGE_REQUIRED", "초기 비밀번호를 변경해야 합니다.");
            return;
        }

        PortalPrincipal refreshedPrincipal = PortalPrincipal.authenticated(account);
        UsernamePasswordAuthenticationToken refreshedAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                refreshedPrincipal,
                null,
                refreshedPrincipal.getAuthorities()
        );
        refreshedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshedAuthentication);
        filterChain.doFilter(request, response);
    }

    private boolean passwordChangeAllowed(String path) {
        return path.equals("/api/auth/me") || path.equals("/api/auth/logout")
                || path.equals("/api/auth/csrf") || path.equals("/api/me/password");
    }

    private void invalidateSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("code", code, "message", message));
    }

    private void writeForbidden(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("code", code, "message", message));
    }
}
