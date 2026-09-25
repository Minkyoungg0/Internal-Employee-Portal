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

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_number", nullable = false, unique = true, length = 20)
    private String employeeNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    protected Employee() {
    }

    public Employee(String employeeNumber, String fullName, LocalDate dateOfBirth, EmploymentStatus employmentStatus) {
        this.employeeNumber = employeeNumber;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.employmentStatus = employmentStatus;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public void terminate() {
        employmentStatus = EmploymentStatus.TERMINATED;
    }
}
