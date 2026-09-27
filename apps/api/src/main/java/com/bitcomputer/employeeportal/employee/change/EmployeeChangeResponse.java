package com.bitcomputer.employeeportal.employee.change;

import com.bitcomputer.employeeportal.employee.Employee;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeChangeResponse(
        Long id,
        Long employeeId,
        String employeeNumber,
        String employeeName,
        PersonalInformation previous,
        PersonalInformation requested,
        EmployeeChangeStatus status,
        Instant requestedAt,
        Instant reviewedAt
) {
    public static EmployeeChangeResponse from(EmployeeChangeHistory history) {
        Employee employee = history.getEmployee();
        return new EmployeeChangeResponse(history.getId(), employee.getId(), employee.getEmployeeNumber(),
                employee.getFullName(),
                PersonalInformation.of(history.getPreviousLastName(), history.getPreviousFirstName(),
                        history.getPreviousDateOfBirth()),
                PersonalInformation.of(history.getRequestedLastName(), history.getRequestedFirstName(),
                        history.getRequestedDateOfBirth()),
                history.getStatus(), history.getRequestedAt(), history.getReviewedAt());
    }

    public record PersonalInformation(String lastName, String firstName, String fullName, LocalDate dateOfBirth) {
        static PersonalInformation of(String lastName, String firstName, LocalDate dateOfBirth) {
            return new PersonalInformation(lastName, firstName, lastName + firstName, dateOfBirth);
        }
    }
}
