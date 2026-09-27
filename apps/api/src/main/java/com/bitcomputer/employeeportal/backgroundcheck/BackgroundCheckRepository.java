package com.bitcomputer.employeeportal.backgroundcheck;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackgroundCheckRepository extends JpaRepository<BackgroundCheck, Long> {
    interface LatestStatus {
        Long getEmployeeId();
        Long getId();
        BackgroundCheckStatus getStatus();
        java.time.Instant getNextPollAt();
        String getTrackingStopReason();
        java.time.Instant getRequestedAt();
    }

    @Query("""
            select c.employee.id as employeeId, c.id as id, c.status as status,
                   c.nextPollAt as nextPollAt, c.trackingStopReason as trackingStopReason,
                   c.requestedAt as requestedAt
            from BackgroundCheck c
            where not exists (select newer.id from BackgroundCheck newer
                where newer.employee.id = c.employee.id
                and (newer.requestedAt > c.requestedAt
                     or (newer.requestedAt = c.requestedAt and newer.id > c.id)))
            """)
    List<LatestStatus> findLatestStatuses();

    @Query("select c from BackgroundCheck c join fetch c.employee where c.nextPollAt <= :now order by c.nextPollAt")
    List<BackgroundCheck> findDue(@Param("now") java.time.Instant now, org.springframework.data.domain.Pageable pageable);

    List<BackgroundCheck> findAllByEmployeeIdOrderByRequestedAtDesc(Long employeeId);
    @Query("select check from BackgroundCheck check join fetch check.employee where check.id = :id and check.employee.id = :employeeId")
    Optional<BackgroundCheck> findByIdAndEmployeeId(@Param("id") Long id, @Param("employeeId") Long employeeId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BackgroundCheck c where c.id = :id and c.employee.id = :employeeId")
    Optional<BackgroundCheck> findForUpdate(@Param("id") Long id, @Param("employeeId") Long employeeId);

    boolean existsByEmployeeIdAndStatusIn(Long employeeId, Collection<BackgroundCheckStatus> statuses);
}
