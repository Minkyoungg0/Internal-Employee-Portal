package com.bitcomputer.employeeportal.employee.dto;

import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheckRepository;
import com.bitcomputer.employeeportal.backgroundcheck.BackgroundCheckStatus;
import java.time.Instant;

public record LatestCheck(Long id, BackgroundCheckStatus status, boolean trackingActive,
                          String trackingStopReason, Instant requestedAt) {
    public static LatestCheck from(BackgroundCheckRepository.LatestStatus c) {
        return new LatestCheck(c.getId(), c.getStatus(), c.getNextPollAt() != null,
                c.getTrackingStopReason(), c.getRequestedAt());
    }
}
