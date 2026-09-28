package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.auth.AccountRole;
import com.bitcomputer.employeeportal.auth.EmployeeAccount;
import com.bitcomputer.employeeportal.auth.EmployeeAccountRepository;
import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheckRepository;
import com.bitcomputer.employeeportal.common.ApiException;
import com.bitcomputer.employeeportal.employee.dto.CreateEmployeeRequest;
import com.bitcomputer.employeeportal.employee.dto.EmployeeDetail;
import com.bitcomputer.employeeportal.employee.dto.EmployeeSummary;
import com.bitcomputer.employeeportal.employee.dto.LatestCheck;
import com.bitcomputer.employeeportal.employee.dto.ProfileResponse;
import com.bitcomputer.employeeportal.employee.dto.TerminationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BackgroundCheckRepository checkRepository;
    private final Clock clock = Clock.systemUTC();

    public EmployeeService(EmployeeRepository employeeRepository, EmployeeAccountRepository accountRepository,
                                   PasswordEncoder passwordEncoder, BackgroundCheckRepository checkRepository) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.checkRepository = checkRepository;
    }

    @Transactional
    public EmployeeDetail create(CreateEmployeeRequest request) {
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

    public List<EmployeeSummary> list() {
        var latest = checkRepository.findLatestStatuses(Instant.now(clock)).stream().collect(
                java.util.stream.Collectors.toMap(BackgroundCheckRepository.LatestStatus::getEmployeeId, LatestCheck::from));
        return employeeRepository.findAllNonAdminEmployeesOrderByEmployeeNumberAsc().stream()
                .map(e -> EmployeeSummary.from(e, latest.get(e.getId()))).toList();
    }

    public EmployeeDetail detail(Long id) {
        Employee employee = findEmployee(id);
        EmployeeAccount account = accountRepository.findByEmployeeId(id).orElse(null);
        return EmployeeDetail.from(employee, account);
    }

    @Transactional
    public EmployeeDetail terminate(Long id, TerminationRequest request) {
        Employee employee = employeeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
        EmployeeAccount account = accountRepository.findByEmployeeId(id).orElse(null);
        if (employee.getEmploymentStatus() == EmploymentStatus.ACTIVE) {
            employee.terminate(request.terminationDate(), Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
            if (account != null) account.disable();
        }
        checkRepository.deleteForEmployee(id);
        return EmployeeDetail.from(employee, account);
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
    }

    public ProfileResponse profile(Long employeeId, String username) {
        return ProfileResponse.from(findEmployee(employeeId), username);
    }
}
