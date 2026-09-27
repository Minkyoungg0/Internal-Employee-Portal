package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.auth.AccountRole;
import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheckRepository;
import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheckStatus;
import com.bitcomputer.employeeportal.auth.EmployeeAccount;
import com.bitcomputer.employeeportal.auth.EmployeeAccountRepository;
import com.bitcomputer.employeeportal.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/admin/employees")
@Tag(name = "관리자 직원 관리")
@SecurityRequirement(name = "sessionCookie")
public class AdminEmployeeController {
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BackgroundCheckRepository checkRepository;
    private final EmployeeChangeService changeService;
    private final Clock clock = Clock.systemUTC();

    public AdminEmployeeController(EmployeeRepository employeeRepository, EmployeeAccountRepository accountRepository,
                                   PasswordEncoder passwordEncoder, BackgroundCheckRepository checkRepository,
                                   EmployeeChangeService changeService) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.checkRepository = checkRepository;
        this.changeService = changeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "직원 계정 생성", description = "직원 인적사항과 로그인 아이디, 초기 비밀번호를 받아 직원 계정을 생성합니다.")
    EmployeeDetail create(@Valid @RequestBody CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmployeeNumber(request.employeeNumber()))
            throw new ApiException(HttpStatus.CONFLICT, "EMPLOYEE_NUMBER_DUPLICATED", "이미 사용 중인 사번입니다.");
        if (accountRepository.existsByUsername(request.username()))
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_DUPLICATED", "이미 사용 중인 아이디입니다.");
        Employee employee = employeeRepository.save(new Employee(request.employeeNumber(), request.lastName(),
                request.firstName(), request.dateOfBirth(), EmploymentStatus.ACTIVE));
        EmployeeAccount account = accountRepository.save(new EmployeeAccount(employee, request.username(),
                passwordEncoder.encode(request.initialPassword()), AccountRole.EMPLOYEE, true));
        return EmployeeDetail.from(employee, account);
    }

    @GetMapping
    @Operation(summary = "전체 직원 조회", description = "관리자 계정을 제외한 전체 직원을 사번순으로 조회하며 가장 최근 Background Check 상태를 함께 반환합니다.")
    List<EmployeeSummary> list() {
        var latest = checkRepository.findLatestStatuses().stream().collect(
                java.util.stream.Collectors.toMap(BackgroundCheckRepository.LatestStatus::getEmployeeId, LatestCheck::from));
        return employeeRepository.findAllNonAdminEmployeesOrderByEmployeeNumberAsc().stream()
                .map(e -> EmployeeSummary.from(e, latest.get(e.getId()))).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "직원 상세 조회", description = "직원 인적사항, 재직 상태, 퇴사 정보와 계정 상태를 조회합니다.")
    EmployeeDetail detail(@PathVariable Long id) {
        Employee employee = findEmployee(id);
        EmployeeAccount account = accountRepository.findByEmployeeId(id).orElse(null);
        return EmployeeDetail.from(employee, account);
    }

    @GetMapping("/{id}/change-history")
    @Operation(summary = "직원 인적사항 변경 이력 조회", description = "지정한 직원의 인적사항 변경 요청과 승인·반려 기록을 최신순으로 조회합니다.")
    List<EmployeeChangeResponse> changeHistory(@PathVariable Long id) {
        findEmployee(id);
        return changeService.listForEmployee(id);
    }

    @PostMapping("/{id}/termination")
    @Transactional
    @Operation(summary = "직원 퇴사 처리", description = "실제 퇴사일과 시스템 처리 시각을 기록하고 해당 직원 계정을 즉시 비활성화합니다.")
    EmployeeDetail terminate(@PathVariable Long id, @Valid @RequestBody TerminationRequest request) {
        Employee employee = employeeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
        EmployeeAccount account = accountRepository.findByEmployeeId(id).orElse(null);
        if (employee.getEmploymentStatus() == EmploymentStatus.ACTIVE) {
            employee.terminate(request.terminationDate(), Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
            if (account != null) account.disable();
        }
        return EmployeeDetail.from(employee, account);
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
    }

    public record CreateEmployeeRequest(
            @NotBlank @Pattern(regexp = "EMP-[0-9]{3,}", message = "사번은 EMP-001 형식이어야 합니다.") String employeeNumber,
            @NotBlank @Size(max = 50) String lastName,
            @NotBlank @Size(max = 50) String firstName,
            LocalDate dateOfBirth,
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(min = 8, max = 72, message = "초기 비밀번호는 8자 이상 72자 이하여야 합니다.") String initialPassword
    ) {}

    public record TerminationRequest(@NotNull(message = "실제 퇴사일을 입력해 주세요.") LocalDate terminationDate) {}

    public record EmployeeSummary(Long id, String employeeNumber, String fullName, LocalDate dateOfBirth,
                                  EmploymentStatus employmentStatus, LatestCheck latestBackgroundCheck) {
        static EmployeeSummary from(Employee e, LatestCheck latest) {
            return new EmployeeSummary(e.getId(), e.getEmployeeNumber(), e.getFullName(), e.getDateOfBirth(), e.getEmploymentStatus(), latest);
        }
    }

    public record LatestCheck(Long id, BackgroundCheckStatus status, boolean trackingActive,
                              String trackingStopReason, Instant requestedAt) {
        static LatestCheck from(BackgroundCheckRepository.LatestStatus c) {
            return new LatestCheck(c.getId(), c.getStatus(), c.getNextPollAt() != null,
                    c.getTrackingStopReason(), c.getRequestedAt());
        }
    }

    public record EmployeeDetail(Long id, String employeeNumber, String lastName, String firstName, String fullName,
                                 LocalDate dateOfBirth, EmploymentStatus employmentStatus, LocalDate terminationDate,
                                 Instant terminatedAt, Account account) {
        static EmployeeDetail from(Employee e, EmployeeAccount a) {
            return new EmployeeDetail(e.getId(), e.getEmployeeNumber(), e.getLastName(), e.getFirstName(), e.getFullName(),
                    e.getDateOfBirth(), e.getEmploymentStatus(), e.getTerminationDate(), e.getTerminatedAt(),
                    a == null ? null : new Account(a.getUsername(), a.getRole(), a.isEnabled(), a.isPasswordChangeRequired()));
        }
    }

    public record Account(String username, AccountRole role, boolean enabled, boolean passwordChangeRequired) {}
}
