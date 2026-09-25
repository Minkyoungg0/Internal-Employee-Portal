package com.bitcomputer.employeeportal.backgroundcheck;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employees/{employeeId}/background-checks")
public class AdminBackgroundCheckController {
    private final BackgroundCheckService service;

    public AdminBackgroundCheckController(BackgroundCheckService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BackgroundCheckResponse start(@PathVariable Long employeeId) {
        return BackgroundCheckResponse.from(service.start(employeeId), true);
    }

    @GetMapping
    List<BackgroundCheckResponse> list(@PathVariable Long employeeId) {
        return service.list(employeeId).stream().map(check -> BackgroundCheckResponse.from(check, false)).toList();
    }

    @GetMapping("/{checkId}")
    BackgroundCheckResponse detail(@PathVariable Long employeeId, @PathVariable Long checkId) {
        return BackgroundCheckResponse.from(service.find(employeeId, checkId), true);
    }

    @PostMapping("/{checkId}/refresh")
    BackgroundCheckResponse refresh(@PathVariable Long employeeId, @PathVariable Long checkId) {
        return BackgroundCheckResponse.from(service.refresh(employeeId, checkId), true);
    }

    record BackgroundCheckResponse(Long id, String externalCheckId, String submittedFirstName,
                                   String submittedLastName, LocalDate submittedDateOfBirth,
                                   BackgroundCheckStatus status, Result result, Instant requestedAt,
                                   Instant completedAt, Instant lastCheckedAt) {
        static BackgroundCheckResponse from(BackgroundCheck check, boolean includeSensitiveResult) {
            Result result = includeSensitiveResult && check.getStatus().isFinal()
                    ? new Result(check.getCriminalRecord(), check.getEducationVerified(),
                    check.getEmploymentVerified(), check.getCreditScore()) : null;
            return new BackgroundCheckResponse(check.getId(), check.getExternalCheckId(),
                    check.getSubmittedFirstName(), check.getSubmittedLastName(), check.getSubmittedDateOfBirth(),
                    check.getStatus(), result, check.getRequestedAt(), check.getCompletedAt(), check.getLastCheckedAt());
        }
    }

    record Result(Boolean criminalRecord, Boolean educationVerified, Boolean employmentVerified, String creditScore) {}
}
