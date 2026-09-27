package com.bitcomputer.employeeportal.employee.dto;

import com.bitcomputer.employeeportal.employee.Employee;
import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import java.time.LocalDate;

public record EmployeeSummary(Long id, String employeeNumber, String fullName, LocalDate dateOfBirth,
                              EmploymentStatus employmentStatus, LatestCheck latestBackgroundCheck) {
    public static EmployeeSummary from(Employee e, LatestCheck latest) {
        return new EmployeeSummary(e.getId(), e.getEmployeeNumber(), e.getFullName(), e.getDateOfBirth(), e.getEmploymentStatus(), latest);
    }
}
