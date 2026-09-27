package com.bitcomputer.employeeportal.backgroundcheck.dto;

import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheck;
import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheckStatus;
import java.time.Instant;
import java.time.LocalDate;

public record BackgroundCheckResponse(Long id, String externalCheckId, String submittedFirstName,
                               String submittedLastName, LocalDate submittedDateOfBirth,
                               BackgroundCheckStatus status, Result result, Instant requestedAt,
                               Instant completedAt, Instant lastCheckedAt, boolean trackingActive, String trackingStopReason) {
    public static BackgroundCheckResponse from(BackgroundCheck check, boolean includeSensitiveResult) {
        Result result = includeSensitiveResult && check.getStatus().isFinal()
                ? new Result(check.getCriminalRecord(), check.getEducationVerified(),
                check.getEmploymentVerified(), check.getCreditScore()) : null;
        return new BackgroundCheckResponse(check.getId(), check.getExternalCheckId(),
                check.getSubmittedFirstName(), check.getSubmittedLastName(), check.getSubmittedDateOfBirth(),
                check.getStatus(), result, check.getRequestedAt(), check.getCompletedAt(), check.getLastCheckedAt(),
                check.getNextPollAt() != null, check.getTrackingStopReason());
    }
}
