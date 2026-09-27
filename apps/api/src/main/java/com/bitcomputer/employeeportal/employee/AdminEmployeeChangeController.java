package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.auth.PortalPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employee-change-requests")
@Tag(name = "관리자 인적사항 승인")
@SecurityRequirement(name = "sessionCookie")
public class AdminEmployeeChangeController {
    private final EmployeeChangeService service;

    public AdminEmployeeChangeController(EmployeeChangeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "전체 인적사항 변경 이력 조회", description = "모든 직원의 인적사항 변경 요청과 승인·반려 이력을 최신순으로 조회합니다.")
    List<EmployeeChangeResponse> list() {
        return service.listAll();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "인적사항 변경 승인", description = "승인 대기 요청을 승인하고 요청한 성, 이름, 생년월일을 직원 정보에 반영합니다.")
    EmployeeChangeResponse approve(@PathVariable Long id, @AuthenticationPrincipal PortalPrincipal principal) {
        return service.review(id, principal.accountId(), true);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "인적사항 변경 반려", description = "승인 대기 요청을 반려하며 현재 직원 정보는 변경하지 않습니다.")
    EmployeeChangeResponse reject(@PathVariable Long id, @AuthenticationPrincipal PortalPrincipal principal) {
        return service.review(id, principal.accountId(), false);
    }
}
