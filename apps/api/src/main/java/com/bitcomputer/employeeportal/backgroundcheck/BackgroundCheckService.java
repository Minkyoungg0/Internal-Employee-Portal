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
        log.info("BACKGROUND_CHECK_REFRESH_REQUESTED checkId={} employeeId={} actorAccountId={}",
                checkId, employeeId, actorAccountId);
        BackgroundCheck check = find(employeeId, checkId);
        if (check.getStatus().isFinal()) {
            log.info("BACKGROUND_CHECK_REFRESH_SKIPPED checkId={} employeeId={} actorAccountId={} status={} reason=ALREADY_FINAL",
                    checkId, employeeId, actorAccountId, check.getStatus());
            return check;
        }
        if (check.getExternalCheckId() == null)
            throw new ApiException(HttpStatus.CONFLICT, "CHECK_ID_UNAVAILABLE", "외부 검사 식별자가 없어 결과를 조회할 수 없습니다.");
        BackgroundCheckStatus previousStatus = check.getStatus();
        try {
            ExternalBackgroundCheck result = client.get(checkId, employeeId, check.getExternalCheckId());
            validateIdentity(check.getEmployee(), result);
            check.apply(result, Instant.now());
            log.info("BACKGROUND_CHECK_STATUS_CHANGED checkId={} employeeId={} actorAccountId={} previousStatus={} newStatus={}",
                    checkId, employeeId, actorAccountId, previousStatus, check.getStatus());
            return repository.save(check);
        } catch (HttpTimeoutException exception) {
            log.warn("BACKGROUND_CHECK_REFRESH_FAILED checkId={} employeeId={} actorAccountId={} errorCode=BACKGROUND_CHECK_TIMEOUT",
                    checkId, employeeId, actorAccountId);
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "BACKGROUND_CHECK_TIMEOUT", "외부 검사 결과 조회 시간이 초과되었습니다.");
        } catch (BackgroundCheckClient.ExternalHttpException exception) {
            log.warn("BACKGROUND_CHECK_REFRESH_FAILED checkId={} employeeId={} actorAccountId={} errorCode=BACKGROUND_CHECK_UNAVAILABLE httpStatus={}",
                    checkId, employeeId, actorAccountId, exception.getStatus());
            HttpStatus status = exception.getStatus() == 503 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
            throw new ApiException(status, "BACKGROUND_CHECK_UNAVAILABLE", "외부 검사 결과를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        } catch (IOException exception) {
            log.warn("BACKGROUND_CHECK_REFRESH_FAILED checkId={} employeeId={} actorAccountId={} errorCode=BACKGROUND_CHECK_INVALID_RESPONSE errorType={}",
                    checkId, employeeId, actorAccountId, exception.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "BACKGROUND_CHECK_INVALID_RESPONSE", "외부 검사 응답을 처리하지 못했습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("BACKGROUND_CHECK_REFRESH_FAILED checkId={} employeeId={} actorAccountId={} errorCode=BACKGROUND_CHECK_INTERRUPTED",
                    checkId, employeeId, actorAccountId);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "BACKGROUND_CHECK_INTERRUPTED", "외부 검사 조회가 중단되었습니다.");
        } catch (IllegalStateException exception) {
            log.warn("BACKGROUND_CHECK_REFRESH_FAILED checkId={} employeeId={} actorAccountId={} errorCode=BACKGROUND_CHECK_INVALID_RESPONSE errorType={}",
                    checkId, employeeId, actorAccountId, exception.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "BACKGROUND_CHECK_INVALID_RESPONSE", "외부 검사 응답을 처리하지 못했습니다.");
        }
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
