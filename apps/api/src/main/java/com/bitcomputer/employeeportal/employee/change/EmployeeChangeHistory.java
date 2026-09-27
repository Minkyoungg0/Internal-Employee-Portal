package com.bitcomputer.employeeportal.employee.change;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employee_change_history")
public class EmployeeChangeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "requested_by_account_id", nullable = false)
    private Long requestedByAccountId;

    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    @Column(name = "previous_last_name", nullable = false, length = 50)
    private String previousLastName;

    @Column(name = "previous_first_name", nullable = false, length = 50)
    private String previousFirstName;

    @Column(name = "previous_date_of_birth")
    private LocalDate previousDateOfBirth;

    @Column(name = "requested_last_name", nullable = false, length = 50)
    private String requestedLastName;

    @Column(name = "requested_first_name", nullable = false, length = 50)
    private String requestedFirstName;

    @Column(name = "requested_date_of_birth")
    private LocalDate requestedDateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeChangeStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected EmployeeChangeHistory() {}

    public EmployeeChangeHistory(Employee employee, Long requestedByAccountId, String requestedLastName,
                                 String requestedFirstName, LocalDate requestedDateOfBirth, Instant requestedAt) {
        this.employee = employee;
        this.requestedByAccountId = requestedByAccountId;
        this.previousLastName = employee.getLastName();
        this.previousFirstName = employee.getFirstName();
        this.previousDateOfBirth = employee.getDateOfBirth();
        this.requestedLastName = requestedLastName;
        this.requestedFirstName = requestedFirstName;
        this.requestedDateOfBirth = requestedDateOfBirth;
        this.status = EmployeeChangeStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public void approve(Long reviewerAccountId, Instant reviewedAt) {
        ensurePending();
        employee.updatePersonalInformation(requestedLastName, requestedFirstName, requestedDateOfBirth);
        status = EmployeeChangeStatus.APPROVED;
        this.reviewedByAccountId = reviewerAccountId;
        this.reviewedAt = reviewedAt;
    }

    public void reject(Long reviewerAccountId, Instant reviewedAt) {
        ensurePending();
        status = EmployeeChangeStatus.REJECTED;
        this.reviewedByAccountId = reviewerAccountId;
        this.reviewedAt = reviewedAt;
    }

    private void ensurePending() {
        if (status != EmployeeChangeStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 변경 요청입니다.");
        }
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public Long getRequestedByAccountId() { return requestedByAccountId; }
    public Long getReviewedByAccountId() { return reviewedByAccountId; }
    public String getPreviousLastName() { return previousLastName; }
    public String getPreviousFirstName() { return previousFirstName; }
    public LocalDate getPreviousDateOfBirth() { return previousDateOfBirth; }
    public String getRequestedLastName() { return requestedLastName; }
    public String getRequestedFirstName() { return requestedFirstName; }
    public LocalDate getRequestedDateOfBirth() { return requestedDateOfBirth; }
    public EmployeeChangeStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
