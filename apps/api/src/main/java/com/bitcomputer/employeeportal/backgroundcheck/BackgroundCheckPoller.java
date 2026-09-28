package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.common.ApiException;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Single-server dispatcher. DB timestamps survive restarts; in-flight IDs prevent overlapping calls. */
@Component
@EnableScheduling
public class BackgroundCheckPoller {
    private static final Logger log = LoggerFactory.getLogger(BackgroundCheckPoller.class);
    private static final int WORKERS = 2;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private final ExecutorService workers = Executors.newFixedThreadPool(WORKERS);
    private final BackgroundCheckRepository repository;
    private final BackgroundCheckService service;

    public BackgroundCheckPoller(BackgroundCheckRepository repository, BackgroundCheckService service) {
        this.repository = repository;
        this.service = service;
    }

    @Scheduled(fixedDelay = 1000)
    public void dispatch() {
        if (inFlight.size() >= WORKERS) return;
        for (BackgroundCheck check : repository.findDue(Instant.now(), PageRequest.of(0, WORKERS * 2))) {
            if (inFlight.size() >= WORKERS) break;
            if (!inFlight.add(check.getId())) continue;
            workers.submit(() -> {
                try {
                    poll(check);
                } catch (Exception exception) {
                    // Do not log external payloads or exception messages.
                    log.error("BACKGROUND_CHECK_POLL_ERROR checkId={} errorType={}",
                            check.getId(), exception.getClass().getSimpleName());
                } finally {
                    inFlight.remove(check.getId());
                }
            });
        }
    }

    void poll(BackgroundCheck check) {
        log.info("BACKGROUND_CHECK_POLL_STARTED checkId={}", check.getId());
        try {
            BackgroundCheck updated = service.refresh(check.getEmployee().getId(), check.getId(), null);
            log.info("BACKGROUND_CHECK_POLL_FINISHED checkId={} trackingActive={} nextPollAt={}",
                    check.getId(), updated.getNextPollAt() != null, updated.getNextPollAt());
        } catch (ApiException exception) {
            // Non-retryable application errors keep the last external status.
            // Deleted/expired rows must never be saved again from this stale snapshot.
            log.warn("BACKGROUND_CHECK_TRACKING_STOPPED checkId={} errorCode={}",
                    check.getId(), exception.getCode());
        }
    }

    @PreDestroy
    void shutdown() {
        workers.shutdownNow();
    }
}
