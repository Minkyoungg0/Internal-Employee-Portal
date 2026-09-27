package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.common.ApiException;
import com.bitcomputer.employeeportal.employee.Employee;
import com.bitcomputer.employeeportal.employee.EmployeeRepository;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackgroundCheckService {
    private static final Logger log = LoggerFactory.getLogger(BackgroundCheckService.class);
    private static final EnumSet<BackgroundCheckStatus> ACTIVE_STATUSES = EnumSet.of(
            BackgroundCheckStatus.REQUESTING, BackgroundCheckStatus.PENDING, BackgroundCheckStatus.SUBMISSION_UNKNOWN);
    private final EmployeeRepository employeeRepository;
    private final BackgroundCheckRepository repository;
    private final BackgroundCheckClient client;
    @org.springframework.beans.factory.annotation.Value("${background-check.retry-budget:180s}")
    private java.time.Duration retryBudget = java.time.Duration.ofSeconds(180);

    public BackgroundCheckService(EmployeeRepository employeeRepository, BackgroundCheckRepository repository,
                                  BackgroundCheckClient client) {
        this.employeeRepository = employeeRepository;
        this.repository = repository;
        this.client = client;
    }

    public synchronized BackgroundCheck start(Long employeeId, Long actorAccountId) {
        log.info("BACKGROUND_CHECK_START_REQUESTED employeeId={} actorAccountId={}", employeeId, actorAccountId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
        if (employee.getDateOfBirth() == null) {
            log.warn("BACKGROUND_CHECK_START_REJECTED employeeId={} actorAccountId={} errorCode=DATE_OF_BIRTH_REQUIRED",
                    employeeId, actorAccountId);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DATE_OF_BIRTH_REQUIRED", "생년월일이 확인되지 않아 검사를 시작할 수 없습니다.");
        }
        if (repository.existsByEmployeeIdAndStatusIn(employeeId, ACTIVE_STATUSES)) {
            log.warn("BACKGROUND_CHECK_START_REJECTED employeeId={} actorAccountId={} errorCode=BACKGROUND_CHECK_IN_PROGRESS",
                    employeeId, actorAccountId);
            throw new ApiException(HttpStatus.CONFLICT, "BACKGROUND_CHECK_IN_PROGRESS", "이미 진행 중이거나 제출 확인이 필요한 검사가 있습니다.");
        }

        BackgroundCheck check = repository.save(new BackgroundCheck(employee, Instant.now()));
        try {
            ExternalBackgroundCheck result = client.create(check.getId(), employeeId, employee.getEmployeeNumber(), employee.getFirstName(),
                    employee.getLastName(), employee.getDateOfBirth());
            validateIdentity(employee, result);
            check.submitted(result, Instant.now());
            log.info("BACKGROUND_CHECK_STATUS_CHANGED checkId={} employeeId={} actorAccountId={} previousStatus=REQUESTING newStatus={}",
                    check.getId(), employeeId, actorAccountId, check.getStatus());
        } catch (BackgroundCheckClient.ExternalHttpException exception) {
            check.submissionFailed();
            log.warn("BACKGROUND_CHECK_STATUS_CHANGED checkId={} employeeId={} actorAccountId={} previousStatus=REQUESTING newStatus=SUBMISSION_FAILED httpStatus={}",
                    check.getId(), employeeId, actorAccountId, exception.getStatus());
        } catch (IOException exception) {
            check.submissionUnknown();
            log.warn("BACKGROUND_CHECK_STATUS_CHANGED checkId={} employeeId={} actorAccountId={} previousStatus=REQUESTING newStatus=SUBMISSION_UNKNOWN errorType={}",
                    check.getId(), employeeId, actorAccountId, exception.getClass().getSimpleName());
        } catch (IllegalStateException exception) {
            check.submissionUnknown();
            log.warn("BACKGROUND_CHECK_STATUS_CHANGED checkId={} employeeId={} actorAccountId={} previousStatus=REQUESTING newStatus=SUBMISSION_UNKNOWN errorType={}",
                    check.getId(), employeeId, actorAccountId, exception.getClass().getSimpleName());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            check.submissionUnknown();
            log.warn("BACKGROUND_CHECK_STATUS_CHANGED checkId={} employeeId={} actorAccountId={} previousStatus=REQUESTING newStatus=SUBMISSION_UNKNOWN errorType=InterruptedException",
                    check.getId(), employeeId, actorAccountId);
        }
        return repository.save(check);
    }

    public BackgroundCheck refresh(Long employeeId, Long checkId, Long actorAccountId) {
        BackgroundCheck check = find(employeeId, checkId);
        if (check.getNextPollAt() == null) return check;
        Instant now = Instant.now();
        java.time.Duration remaining = check.getRetryDeadlineAt() == null ? null
                : java.time.Duration.between(now, check.getRetryDeadlineAt());
        if (remaining != null && (remaining.isNegative() || remaining.isZero())) {
            check.stopTracking("RETRY_TIME_EXHAUSTED");
            return repository.save(check);
        }
        try {
            ExternalBackgroundCheck result = client.get(checkId, employeeId, check.getExternalCheckId(), remaining);
            validateIdentity(check.getEmployee(), result);
            if (!check.getExternalCheckId().equals(result.checkId()))
                throw new IOException("검사 식별자가 일치하지 않습니다.");
            if (check.getRetryDeadlineAt() != null && !Instant.now().isBefore(check.getRetryDeadlineAt())) {
                check.stopTracking("RETRY_TIME_EXHAUSTED");
            } else {
                check.apply(result, Instant.now());
            }
        } catch (BackgroundCheckClient.ExternalHttpException exception) {
            if (exception.getRetryDelay() != null) {
                check.retryAfter(Instant.now(), exception.getRetryDelay(), retryBudget);
                log.warn("BACKGROUND_CHECK_RETRY_SCHEDULED checkId={} httpStatus={} nextPollAt={} retryDeadlineAt={}",
                        checkId, exception.getStatus(), check.getNextPollAt(), check.getRetryDeadlineAt());
            } else {
                check.stopTracking("EXTERNAL_HTTP_" + exception.getStatus());
            }
        } catch (HttpTimeoutException exception) {
            if (check.getRetryDeadlineAt() != null && !Instant.now().isBefore(check.getRetryDeadlineAt()))
                check.stopTracking("RETRY_TIME_EXHAUSTED");
            else check.stopTracking("BACKGROUND_CHECK_TIMEOUT");
        } catch (IOException | IllegalStateException exception) {
            check.stopTracking("BACKGROUND_CHECK_INVALID_RESPONSE");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            // Keep the persisted schedule for restart recovery.
            return check;
        }
        log.info("BACKGROUND_CHECK_POLL_STATE checkId={} status={} trackingActive={} stopReason={}",
                checkId, check.getStatus(), check.getNextPollAt() != null, check.getTrackingStopReason());
        return repository.save(check);
    }

    @Transactional
    public BackgroundCheck retry(Long employeeId, Long checkId, Long actorAccountId) {
        BackgroundCheck check = repository.findForUpdate(checkId, employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BACKGROUND_CHECK_NOT_FOUND", "검사 이력을 찾을 수 없습니다."));
        if (check.getExternalCheckId() == null)
            throw new ApiException(HttpStatus.CONFLICT, "CHECK_ID_UNAVAILABLE", "외부 검사 식별자가 없어 재시도할 수 없습니다.");
        if (check.getNextPollAt() != null) return check;
        if (check.getTrackingStopReason() == null)
            throw new ApiException(HttpStatus.CONFLICT, "CHECK_NOT_STOPPED", "중단된 검사만 재시도할 수 있습니다.");
        check.resumeTracking(Instant.now());
        log.info("BACKGROUND_CHECK_TRACKING_RESUMED checkId={} employeeId={} actorAccountId={}", checkId, employeeId, actorAccountId);
        return check;
    }

    @Transactional(readOnly = true)
    public List<BackgroundCheck> list(Long employeeId) {
        ensureEmployee(employeeId);
        return repository.findAllByEmployeeIdOrderByRequestedAtDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public BackgroundCheck find(Long employeeId, Long checkId) {
        return repository.findByIdAndEmployeeId(checkId, employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BACKGROUND_CHECK_NOT_FOUND", "검사 이력을 찾을 수 없습니다."));
    }

    private void ensureEmployee(Long id) {
        if (!employeeRepository.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다.");
    }

    private void validateIdentity(Employee employee, ExternalBackgroundCheck result) throws IOException {
        if (!employee.getEmployeeNumber().equals(result.employeeId()))
            throw new IOException("Background Check 응답의 employeeId가 요청과 다릅니다.");
    }
}
