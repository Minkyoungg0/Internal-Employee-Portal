package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeChangeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeChangeHistoryRepository historyRepository;
    private final Clock clock = Clock.systemUTC();

    public EmployeeChangeService(EmployeeRepository employeeRepository, EmployeeChangeHistoryRepository historyRepository) {
        this.employeeRepository = employeeRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public EmployeeChangeHistory request(Long employeeId, Long accountId, String lastName, String firstName,
                                         LocalDate dateOfBirth) {
        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
        if (historyRepository.existsByEmployeeIdAndStatus(employeeId, EmployeeChangeStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "CHANGE_REQUEST_PENDING", "승인 대기 중인 변경 요청이 있습니다.");
        }
        if (employee.getLastName().equals(lastName) && employee.getFirstName().equals(firstName)
                && Objects.equals(employee.getDateOfBirth(), dateOfBirth)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PERSONAL_INFORMATION_UNCHANGED", "변경된 인적사항이 없습니다.");
        }
        return historyRepository.save(new EmployeeChangeHistory(employee, accountId, lastName, firstName,
                dateOfBirth, now()));
    }

    @Transactional(readOnly = true)
    public List<EmployeeChangeHistory> listForEmployee(Long employeeId) {
        return historyRepository.findAllByEmployeeIdOrderByRequestedAtDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeChangeHistory> listAll() {
        return historyRepository.findAllByOrderByRequestedAtDesc();
    }

    @Transactional
    public EmployeeChangeHistory review(Long historyId, Long reviewerAccountId, boolean approve) {
        EmployeeChangeHistory history = historyRepository.findByIdForUpdate(historyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHANGE_REQUEST_NOT_FOUND", "변경 요청을 찾을 수 없습니다."));
        if (history.getStatus() != EmployeeChangeStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "CHANGE_REQUEST_ALREADY_REVIEWED", "이미 처리된 변경 요청입니다.");
        }
        if (approve) history.approve(reviewerAccountId, now());
        else history.reject(reviewerAccountId, now());
        return history;
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
