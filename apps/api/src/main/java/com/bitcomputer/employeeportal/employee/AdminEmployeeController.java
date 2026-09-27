package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.employee.change.EmployeeChangeResponse;
import com.bitcomputer.employeeportal.employee.change.EmployeeChangeService;
import com.bitcomputer.employeeportal.employee.dto.CreateEmployeeRequest;
import com.bitcomputer.employeeportal.employee.dto.EmployeeDetail;
import com.bitcomputer.employeeportal.employee.dto.EmployeeSummary;
import com.bitcomputer.employeeportal.employee.dto.TerminationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employees")
@Tag(name = "관리자 직원 관리")
@SecurityRequirement(name = "sessionCookie")
public class AdminEmployeeController {
    private final EmployeeService employeeService;
    private final EmployeeChangeService changeService;

    public AdminEmployeeController(EmployeeService employeeService, EmployeeChangeService changeService) {
        this.employeeService = employeeService;
        this.changeService = changeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "직원 계정 생성", description = "직원 인적사항과 로그인 아이디, 초기 비밀번호를 받아 직원 계정을 생성합니다.")
    EmployeeDetail create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @GetMapping
    @Operation(summary = "전체 직원 조회", description = "관리자 계정을 제외한 전체 직원을 사번순으로 조회하며 가장 최근 Background Check 상태를 함께 반환합니다.")
    List<EmployeeSummary> list() {
        return employeeService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "직원 상세 조회", description = "직원 인적사항, 재직 상태, 퇴사 정보와 계정 상태를 조회합니다.")
    EmployeeDetail detail(@PathVariable Long id) {
        return employeeService.detail(id);
    }

    @GetMapping("/{id}/change-history")
    @Operation(summary = "직원 인적사항 변경 이력 조회", description = "지정한 직원의 인적사항 변경 요청과 승인·반려 기록을 최신순으로 조회합니다.")
    List<EmployeeChangeResponse> changeHistory(@PathVariable Long id) {
        employeeService.detail(id);
        return changeService.listForEmployee(id);
    }

    @PostMapping("/{id}/termination")
    @Operation(summary = "직원 퇴사 처리", description = "실제 퇴사일과 시스템 처리 시각을 기록하고 해당 직원 계정을 즉시 비활성화합니다.")
    EmployeeDetail terminate(@PathVariable Long id, @Valid @RequestBody TerminationRequest request) {
        return employeeService.terminate(id, request);
    }

}
