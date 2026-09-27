package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.employee.change.EmployeeChangeService;

import com.bitcomputer.employeeportal.employee.change.EmployeeChangeHistory;

import com.bitcomputer.employeeportal.employee.change.EmployeeChangeStatus;

import com.bitcomputer.employeeportal.employee.change.EmployeeChangeResponse;

import com.bitcomputer.employeeportal.employee.change.EmployeeChangeHistoryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bitcomputer.employeeportal.common.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmployeeChangeServiceTest {
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final EmployeeChangeHistoryRepository historyRepository = mock(EmployeeChangeHistoryRepository.class);
    private final EmployeeChangeService service = new EmployeeChangeService(employeeRepository, historyRepository);

    @Test
    void pendingRequestPreventsAnotherRequest() {
        Employee employee = employee();
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(employee));
        when(historyRepository.existsByEmployeeIdAndStatus(1L, EmployeeChangeStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.request(1L, 10L, "이", "민준", LocalDate.of(1990, 3, 15)))
                .isInstanceOf(ApiException.class)
                .hasMessage("승인 대기 중인 변경 요청이 있습니다.");
    }

    @Test
    void approvalAppliesRequestedInformationAndCompletesHistory() {
        Employee employee = employee();
        EmployeeChangeHistory history = new EmployeeChangeHistory(employee, 10L, "이", "민준",
                LocalDate.of(1991, 4, 16), Instant.now());
        when(historyRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(history));

        EmployeeChangeResponse approved = service.review(3L, 20L, true);

        assertThat(approved.status()).isEqualTo(EmployeeChangeStatus.APPROVED);
        assertThat(employee.getFullName()).isEqualTo("이민준");
        assertThat(employee.getDateOfBirth()).isEqualTo(LocalDate.of(1991, 4, 16));
        verifyNoInteractions(employeeRepository);
    }

    private Employee employee() {
        return new Employee("EMP-001", "김", "민준", LocalDate.of(1990, 3, 15), EmploymentStatus.ACTIVE);
    }
}
