package com.bitcomputer.employeeportal.employee.dto;

import com.bitcomputer.employeeportal.employee.Employee;
import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import java.time.LocalDate;

public record ProfileResponse(Long id, String employeeNumber, String lastName, String firstName,
                              String fullName, java.time.LocalDate dateOfBirth, EmploymentStatus employmentStatus,
                              String username) {
    public static ProfileResponse from(Employee employee, String username) {
        return new ProfileResponse(employee.getId(), employee.getEmployeeNumber(), employee.getLastName(),
                employee.getFirstName(), employee.getFullName(), employee.getDateOfBirth(),
                employee.getEmploymentStatus(), username);
    }
}
