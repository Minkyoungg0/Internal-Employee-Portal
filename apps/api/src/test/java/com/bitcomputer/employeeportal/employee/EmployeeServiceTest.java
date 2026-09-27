package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.auth.*;
import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheckRepository;
import com.bitcomputer.employeeportal.employee.dto.TerminationRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {
    @Test
    void repeatedTerminationKeepsOriginalDatesAndDisablesAccount() {
        var employees = mock(EmployeeRepository.class);
        var accounts = mock(EmployeeAccountRepository.class);
        var service = new EmployeeService(employees, accounts, mock(PasswordEncoder.class),
                mock(BackgroundCheckRepository.class));
        var employee = new Employee("EMP-001", "김", "민준", null, EmploymentStatus.ACTIVE);
        var account = new EmployeeAccount(employee, "employee", "hash", AccountRole.EMPLOYEE, false);
        when(employees.findByIdForUpdate(1L)).thenReturn(Optional.of(employee));
        when(accounts.findByEmployeeId(1L)).thenReturn(Optional.of(account));
        var first = service.terminate(1L, new TerminationRequest(LocalDate.of(2026, 9, 27)));
        var repeated = service.terminate(1L, new TerminationRequest(LocalDate.of(2026, 9, 28)));
        assertThat(repeated.terminationDate()).isEqualTo(first.terminationDate());
        assertThat(repeated.terminatedAt()).isEqualTo(first.terminatedAt());
        assertThat(account.isEnabled()).isFalse();
        assertThat(repeated.employmentStatus()).isEqualTo(EmploymentStatus.TERMINATED);
    }
}
