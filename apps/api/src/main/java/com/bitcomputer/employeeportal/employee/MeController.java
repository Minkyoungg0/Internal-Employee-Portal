package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.auth.PortalPrincipal;
import com.bitcomputer.employeeportal.auth.EmployeeAccount;
import com.bitcomputer.employeeportal.auth.EmployeeAccountRepository;
import com.bitcomputer.employeeportal.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeChangeService changeService;

    public MeController(EmployeeRepository employeeRepository, EmployeeAccountRepository accountRepository,
                        PasswordEncoder passwordEncoder, EmployeeChangeService changeService) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.changeService = changeService;
    }

    @PostMapping("/profile-change-requests")
    @ResponseStatus(HttpStatus.CREATED)
    EmployeeChangeResponse requestProfileChange(@AuthenticationPrincipal PortalPrincipal principal,
                                                @Valid @RequestBody ProfileChangeRequest request) {
        return changeService.request(principal.employeeId(), principal.accountId(), request.lastName(),
                request.firstName(), request.dateOfBirth());
    }

    @GetMapping("/profile-change-requests")
    List<EmployeeChangeResponse> profileChangeHistory(@AuthenticationPrincipal PortalPrincipal principal) {
        return changeService.listForEmployee(principal.employeeId());
    }

    @GetMapping("/profile")
    ProfileResponse profile(@AuthenticationPrincipal PortalPrincipal principal) {
        Employee employee = employeeRepository.findById(principal.employeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
        return ProfileResponse.from(employee, principal.getUsername());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void changePassword(@AuthenticationPrincipal PortalPrincipal principal,
                        @Valid @RequestBody ChangePasswordRequest request) {
        EmployeeAccount account = accountRepository.findById(principal.accountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED", "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
        account.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "현재 비밀번호를 입력해 주세요.") String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "새 비밀번호는 8자 이상 72자 이하여야 합니다.") String newPassword
    ) {}

    public record ProfileChangeRequest(
            @NotBlank @Size(max = 50) String lastName,
            @NotBlank @Size(max = 50) String firstName,
            LocalDate dateOfBirth
    ) {}

    public record ProfileResponse(Long id, String employeeNumber, String lastName, String firstName,
                                  String fullName, java.time.LocalDate dateOfBirth, EmploymentStatus employmentStatus,
                                  String username) {
        static ProfileResponse from(Employee employee, String username) {
            return new ProfileResponse(employee.getId(), employee.getEmployeeNumber(), employee.getLastName(),
                    employee.getFirstName(), employee.getFullName(), employee.getDateOfBirth(),
                    employee.getEmploymentStatus(), username);
        }
    }
}
