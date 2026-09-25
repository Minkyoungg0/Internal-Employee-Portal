package com.bitcomputer.employeeportal.backgroundcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitcomputer.employeeportal.common.ApiException;
import com.bitcomputer.employeeportal.employee.Employee;
import com.bitcomputer.employeeportal.employee.EmployeeRepository;
import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BackgroundCheckServiceTest {
    @Mock EmployeeRepository employeeRepository;
    @Mock BackgroundCheckRepository repository;
    @Mock BackgroundCheckClient client;
    BackgroundCheckService service;
    Employee employee;

    @BeforeEach
    void setUp() {
        service = new BackgroundCheckService(employeeRepository, repository, client);
        employee = new Employee("EMP-003", "남궁", "서준", LocalDate.of(1988, 7, 21), EmploymentStatus.ACTIVE);
        ReflectionTestUtils.setField(employee, "id", 3L);
    }

    @Test
    void sendsStoredSeparatedNameAndKeepsPendingRequest() throws Exception {
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(repository.existsByEmployeeIdAndStatusIn(any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(client.create("EMP-003", "서준", "남궁", LocalDate.of(1988, 7, 21)))
                .thenReturn(new ExternalBackgroundCheck("CHK-1", "EMP-003", "pending", null, null, null, null, null));

        BackgroundCheck result = service.start(3L);

        assertEquals(BackgroundCheckStatus.PENDING, result.getStatus());
        assertEquals("남궁", result.getSubmittedLastName());
        assertEquals("서준", result.getSubmittedFirstName());
        verify(client).create("EMP-003", "서준", "남궁", LocalDate.of(1988, 7, 21));
    }

    @Test
    void rejectsEmployeeWithoutDateOfBirthBeforeExternalCall() throws Exception {
        employee = new Employee("EMP-007", "이", "서연", null, EmploymentStatus.ACTIVE);
        ReflectionTestUtils.setField(employee, "id", 7L);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));

        ApiException exception = assertThrows(ApiException.class, () -> service.start(7L));

        assertEquals("DATE_OF_BIRTH_REQUIRED", exception.getCode());
        verify(client, never()).create(any(), any(), any(), any());
    }

    @Test
    void completedResultIsReturnedWithoutCallingExternalApiAgain() throws Exception {
        BackgroundCheck check = new BackgroundCheck(employee, Instant.now());
        check.submitted(new ExternalBackgroundCheck("CHK-1", "EMP-003", "clear", false, true, true, "good", Instant.now()), Instant.now());
        when(repository.findByIdAndEmployeeId(8L, 3L)).thenReturn(Optional.of(check));

        BackgroundCheck result = service.refresh(3L, 8L);

        assertEquals(BackgroundCheckStatus.CLEAR, result.getStatus());
        verify(client, never()).get(any());
    }
}
