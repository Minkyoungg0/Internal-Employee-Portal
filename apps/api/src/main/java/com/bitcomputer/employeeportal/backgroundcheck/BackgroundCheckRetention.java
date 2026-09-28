package com.bitcomputer.employeeportal.backgroundcheck;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class BackgroundCheckRetention {
    private static final Logger log = LoggerFactory.getLogger(BackgroundCheckRetention.class);
    private final BackgroundCheckRepository repository;
    private final TransactionTemplate transactions;

    public BackgroundCheckRetention(BackgroundCheckRepository repository, PlatformTransactionManager manager) {
        this.repository = repository;
        this.transactions = new TransactionTemplate(manager);
    }

    // Each batch commits independently; failures leave remaining rows for the next run.
    @Scheduled(fixedDelayString = "${background-check.retention-cleanup-delay:1d}")
    public void purgeExpired() {
        Instant cutoff = Instant.now();
        int total = 0;
        try {
            int deleted;
            do {
                deleted = transactions.execute(tx -> repository.deleteExpiredBatch(cutoff));
                total += deleted;
            } while (deleted == 200);
            log.info("BACKGROUND_CHECK_RETENTION_PURGED count={}", total);
        } catch (RuntimeException exception) {
            log.error("BACKGROUND_CHECK_RETENTION_FAILED deletedCount={} errorType={}",
                    total, exception.getClass().getSimpleName());
        }
    }
}
