package com.bitcomputer.employeeportal.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @GetMapping("/me")
    CurrentUserResponse me(@AuthenticationPrincipal PortalPrincipal principal) {
        return CurrentUserResponse.from(principal);
    }

    public record CsrfResponse(String headerName, String parameterName, String token) {
    }

    public record CurrentUserResponse(
            Long accountId,
            Long employeeId,
            String employeeNumber,
            String username,
            AccountRole role,
            boolean passwordChangeRequired
    ) {
        public static CurrentUserResponse from(PortalPrincipal principal) {
            return new CurrentUserResponse(
                    principal.accountId(),
                    principal.employeeId(),
                    principal.employeeNumber(),
                    principal.getUsername(),
                    principal.role(),
                    principal.passwordChangeRequired()
            );
        }
    }
}
