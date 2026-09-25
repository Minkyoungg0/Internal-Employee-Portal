package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.employee.Employee;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "background_check")
public class BackgroundCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @Column(name = "external_check_id", unique = true, length = 100)
    private String externalCheckId;
    @Column(name = "submitted_first_name", nullable = false, length = 50)
    private String submittedFirstName;
    @Column(name = "submitted_last_name", nullable = false, length = 50)
    private String submittedLastName;
    @Column(name = "submitted_date_of_birth", nullable = false)
    private LocalDate submittedDateOfBirth;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private BackgroundCheckStatus status;
    @Column(name = "criminal_record") private Boolean criminalRecord;
    @Column(name = "education_verified") private Boolean educationVerified;
    @Column(name = "employment_verified") private Boolean employmentVerified;
    @Column(name = "credit_score", length = 20) private String creditScore;
    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "last_checked_at") private Instant lastCheckedAt;

    protected BackgroundCheck() {}

    public BackgroundCheck(Employee employee, Instant now) {
        this.employee = employee;
        this.submittedFirstName = employee.getFirstName();
        this.submittedLastName = employee.getLastName();
        this.submittedDateOfBirth = employee.getDateOfBirth();
        this.status = BackgroundCheckStatus.REQUESTING;
        this.requestedAt = now;
    }

    public void submitted(ExternalBackgroundCheck result, Instant checkedAt) {
        this.externalCheckId = result.checkId();
        apply(result, checkedAt);
    }

    public void apply(ExternalBackgroundCheck result, Instant checkedAt) {
        BackgroundCheckStatus received;
        try {
            received = BackgroundCheckStatus.valueOf(result.status().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("알 수 없는 Background Check 상태입니다.", exception);
        }
        this.status = received;
        this.lastCheckedAt = checkedAt;
        if (received.isFinal()) {
            criminalRecord = result.criminalRecord();
            educationVerified = result.educationVerified();
            employmentVerified = result.employmentVerified();
            creditScore = result.creditScore();
            completedAt = result.completedAt() == null ? checkedAt : result.completedAt();
        }
    }

    public void submissionUnknown() { status = BackgroundCheckStatus.SUBMISSION_UNKNOWN; }
    public void submissionFailed() { status = BackgroundCheckStatus.SUBMISSION_FAILED; }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public String getExternalCheckId() { return externalCheckId; }
    public String getSubmittedFirstName() { return submittedFirstName; }
    public String getSubmittedLastName() { return submittedLastName; }
    public LocalDate getSubmittedDateOfBirth() { return submittedDateOfBirth; }
    public BackgroundCheckStatus getStatus() { return status; }
    public Boolean getCriminalRecord() { return criminalRecord; }
    public Boolean getEducationVerified() { return educationVerified; }
    public Boolean getEmploymentVerified() { return employmentVerified; }
    public String getCreditScore() { return creditScore; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
}
