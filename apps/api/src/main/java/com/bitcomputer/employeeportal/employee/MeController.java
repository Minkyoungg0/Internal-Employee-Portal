package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.auth.PasswordService;
import com.bitcomputer.employeeportal.auth.PortalPrincipal;
import com.bitcomputer.employeeportal.auth.dto.ChangePasswordRequest;
import com.bitcomputer.employeeportal.employee.change.EmployeeChangeResponse;
import com.bitcomputer.employeeportal.employee.change.EmployeeChangeService;
import com.bitcomputer.employeeportal.employee.dto.ProfileChangeRequest;
import com.bitcomputer.employeeportal.employee.dto.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "직원 본인 정보")
@SecurityRequirement(name = "sessionCookie")
public class MeController {
    private final EmployeeService employeeService;
    private final PasswordService passwordService;
    private final EmployeeChangeService changeService;

    public MeController(EmployeeService employeeService,
                        PasswordService passwordService,
                        EmployeeChangeService changeService) {
        this.employeeService = employeeService;
        this.passwordService = passwordService;
        this.changeService = changeService;
    }

    @PostMapping("/profile-change-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "인적사항 변경 요청", description = "성, 이름, 생년월일 변경을 관리자에게 요청합니다. 승인 대기 요청은 한 건만 허용됩니다.")
    EmployeeChangeResponse requestProfileChange(@AuthenticationPrincipal PortalPrincipal principal,
                                                @Valid @RequestBody ProfileChangeRequest request) {
        return changeService.request(principal.employeeId(), principal.accountId(), request.lastName(),
                request.firstName(), request.dateOfBirth());
    }

    @GetMapping("/profile-change-requests")
    @Operation(summary = "내 인적사항 변경 이력 조회", description = "본인이 요청한 인적사항 변경의 승인 대기, 승인 완료, 반려 상태를 조회합니다.")
    List<EmployeeChangeResponse> profileChangeHistory(@AuthenticationPrincipal PortalPrincipal principal) {
        return changeService.listForEmployee(principal.employeeId());
    }

    @GetMapping("/profile")
    @Operation(summary = "내 인적사항 조회", description = "로그인한 직원의 사번, 성명, 생년월일, 재직 상태와 로그인 아이디를 조회합니다.")
    ProfileResponse profile(@AuthenticationPrincipal PortalPrincipal principal) {
        return employeeService.profile(principal.employeeId(), principal.getUsername());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 즉시 변경합니다. 관리자 승인 대상이 아니며 변경 이력에 저장하지 않습니다.")
    void changePassword(@AuthenticationPrincipal PortalPrincipal principal,
                        @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(principal.accountId(), request);
    }

}
