package com.bitcomputer.employeeportal.employee.dto;

import com.bitcomputer.employeeportal.auth.EmployeeAccount;
import com.bitcomputer.employeeportal.employee.Employee;
import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeDetail(Long id, String employeeNumber, String lastName, String firstName, String fullName,
                             LocalDate dateOfBirth, EmploymentStatus employmentStatus, LocalDate terminationDate,
                             Instant terminatedAt, Account account) {
    public static EmployeeDetail from(Employee e, EmployeeAccount a) {
        return new EmployeeDetail(e.getId(), e.getEmployeeNumber(), e.getLastName(), e.getFirstName(), e.getFullName(),
                e.getDateOfBirth(), e.getEmploymentStatus(), e.getTerminationDate(), e.getTerminatedAt(),
                a == null ? null : new Account(a.getUsername(), a.getRole(), a.isEnabled(), a.isPasswordChangeRequired()));
    }
}
