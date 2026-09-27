package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.backgroundcheck.client.ExternalBackgroundCheck;
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

    @Column(name = "next_poll_at") private Instant nextPollAt;
    @Column(name = "tracking_stop_reason", length = 100) private String trackingStopReason;

    public Instant getNextPollAt() { return nextPollAt; }
    public String getTrackingStopReason() { return trackingStopReason; }
    public void schedulePoll(Instant when) { nextPollAt = when; trackingStopReason = null; }
    public void stopTracking(String reason) { nextPollAt = null; trackingStopReason = reason; }

    @Column(name = "retry_deadline_at") private Instant retryDeadlineAt;
    public Instant getRetryDeadlineAt() { return retryDeadlineAt; }
    public void resumeTracking(Instant now) { retryDeadlineAt = null; schedulePoll(now); }
    public void retryAfter(Instant now, java.time.Duration delay, java.time.Duration budget) {
        if (retryDeadlineAt == null) retryDeadlineAt = now.plus(budget);
        Instant next = now.plus(delay);
        if (!next.isBefore(retryDeadlineAt)) {
            // Wake at the deadline to mark stopped, without shortening the server's wait.
            nextPollAt = retryDeadlineAt;
        } else {
            nextPollAt = next;
        }
        trackingStopReason = null;
    }

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
        // POST can report completion without detailed result fields.
        if (result.criminalRecord() == null || result.educationVerified() == null
                || result.employmentVerified() == null || result.creditScore() == null) {
            schedulePoll(checkedAt.plusSeconds(15));
        }
    }

    public void apply(ExternalBackgroundCheck result, Instant checkedAt) {
        BackgroundCheckStatus received;
        try {
            received = BackgroundCheckStatus.valueOf(result.status().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("알 수 없는 Background Check 상태입니다.", exception);
        }
        if (received != BackgroundCheckStatus.PENDING && !received.isFinal()) {
            throw new IllegalStateException("알 수 없는 외부 검사 상태입니다.");
        }
        retryDeadlineAt = null;
        this.status = received;
        nextPollAt = received == BackgroundCheckStatus.PENDING ? checkedAt.plusSeconds(15) : null;
        trackingStopReason = null;
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
