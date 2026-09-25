package com.bitcomputer.employeeportal.auth;

import com.bitcomputer.employeeportal.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_account")
public class EmployeeAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    protected EmployeeAccount() {
    }

    public EmployeeAccount(Employee employee, String username, String passwordHash, AccountRole role, boolean enabled) {
        this.employee = employee;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
        this.passwordChangeRequired = true;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangeRequired = false;
    }

    public void disable() {
        enabled = false;
    }
}
