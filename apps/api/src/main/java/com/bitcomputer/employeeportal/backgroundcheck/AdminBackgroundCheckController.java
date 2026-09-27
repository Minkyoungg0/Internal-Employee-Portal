package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.auth.PortalPrincipal;
import com.bitcomputer.employeeportal.backgroundcheck.dto.BackgroundCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employees/{employeeId}/background-checks")
@Tag(name = "관리자 Background Check")
@SecurityRequirement(name = "sessionCookie")
public class AdminBackgroundCheckController {
    private static final Logger log = LoggerFactory.getLogger(AdminBackgroundCheckController.class);
    private final BackgroundCheckService service;

    public AdminBackgroundCheckController(BackgroundCheckService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Background Check 시작", description = "직원 정보로 외부 검사를 한 번 접수하고 자동 결과 추적을 시작합니다. 관리자만 실행할 수 있습니다.")
    BackgroundCheckResponse start(@PathVariable Long employeeId, @AuthenticationPrincipal PortalPrincipal principal) {
        return BackgroundCheckResponse.from(service.start(employeeId, principal.accountId()), true);
    }

    @GetMapping
    @Operation(summary = "검사 이력 조회", description = "직원의 검사 이력을 최신순으로 조회합니다. 목록 응답에는 민감한 상세 결과가 포함되지 않습니다.")
    List<BackgroundCheckResponse> list(@PathVariable Long employeeId) {
        return service.list(employeeId).stream().map(check -> BackgroundCheckResponse.from(check, false)).toList();
    }

    @GetMapping("/{checkId}")
    @Operation(summary = "검사 상세 결과 조회", description = "저장된 검사 상태와 범죄 기록, 학력·경력 확인, 신용 점수 등 상세 결과를 조회합니다.")
    BackgroundCheckResponse detail(@PathVariable Long employeeId, @PathVariable Long checkId,
                                   @AuthenticationPrincipal PortalPrincipal principal) {
        log.info("BACKGROUND_CHECK_RESULT_VIEWED checkId={} employeeId={} actorAccountId={}",
                checkId, employeeId, principal.accountId());
        return BackgroundCheckResponse.from(service.find(employeeId, checkId), true);
    }

    @PostMapping("/{checkId}/retry")
    @Operation(summary = "검사 결과 추적 재시도", description = "자동 추적이 중단된 검사를 기존 외부 checkId로 다시 조회하도록 예약합니다.")
    BackgroundCheckResponse retry(@PathVariable Long employeeId, @PathVariable Long checkId,
                                  @AuthenticationPrincipal PortalPrincipal principal) {
        return BackgroundCheckResponse.from(service.retry(employeeId, checkId, principal.accountId()), true);
    }

}
