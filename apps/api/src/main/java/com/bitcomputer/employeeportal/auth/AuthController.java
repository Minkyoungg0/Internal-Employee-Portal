package com.bitcomputer.employeeportal.auth;

import com.bitcomputer.employeeportal.auth.dto.CsrfResponse;
import com.bitcomputer.employeeportal.auth.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증")
public class AuthController {
    @GetMapping("/csrf")
    @Operation(summary = "CSRF 토큰 발급", description = "상태 변경 요청에 사용할 헤더 이름과 CSRF 토큰을 발급합니다.")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @GetMapping("/me")
    @Operation(summary = "현재 로그인 정보 조회", description = "현재 세션의 계정, 직원, 역할과 초기 비밀번호 변경 필요 여부를 조회합니다.",
            security = @SecurityRequirement(name = "sessionCookie"))
    CurrentUserResponse me(@AuthenticationPrincipal PortalPrincipal principal) {
        return CurrentUserResponse.from(principal);
    }

}
