package com.bitcomputer.employeeportal.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_number", nullable = false, unique = true, length = 20)
    private String employeeNumber;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    protected Employee() {
    }

    public Employee(String employeeNumber, String lastName, String firstName, LocalDate dateOfBirth, EmploymentStatus employmentStatus) {
        this.employeeNumber = employeeNumber;
        this.lastName = lastName;
        this.firstName = firstName;
        this.fullName = lastName + firstName;
        this.dateOfBirth = dateOfBirth;
        this.employmentStatus = employmentStatus;
    }

    public Employee(String employeeNumber, String fullName, LocalDate dateOfBirth, EmploymentStatus employmentStatus) {
        this(employeeNumber, fullName.substring(0, 1), fullName.substring(1), dateOfBirth, employmentStatus);
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getLastName() { return lastName; }

    public String getFirstName() { return firstName; }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public LocalDate getTerminationDate() { return terminationDate; }

    public Instant getTerminatedAt() { return terminatedAt; }

    public void terminate(LocalDate terminationDate, Instant terminatedAt) {
        employmentStatus = EmploymentStatus.TERMINATED;
        this.terminationDate = terminationDate;
        this.terminatedAt = terminatedAt;
    }

    public void terminate() { terminate(LocalDate.now(), Instant.now()); }

    public void updatePersonalInformation(String lastName, String firstName, LocalDate dateOfBirth) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.fullName = lastName + firstName;
        this.dateOfBirth = dateOfBirth;
    }
}
