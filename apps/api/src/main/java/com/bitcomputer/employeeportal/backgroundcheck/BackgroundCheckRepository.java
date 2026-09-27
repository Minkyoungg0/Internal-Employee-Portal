package com.bitcomputer.employeeportal.backgroundcheck;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackgroundCheckRepository extends JpaRepository<BackgroundCheck, Long> {
    @Query("select c from BackgroundCheck c join fetch c.employee where c.nextPollAt <= :now order by c.nextPollAt")
    List<BackgroundCheck> findDue(@Param("now") java.time.Instant now, org.springframework.data.domain.Pageable pageable);

    List<BackgroundCheck> findAllByEmployeeIdOrderByRequestedAtDesc(Long employeeId);
    @Query("select check from BackgroundCheck check join fetch check.employee where check.id = :id and check.employee.id = :employeeId")
    Optional<BackgroundCheck> findByIdAndEmployeeId(@Param("id") Long id, @Param("employeeId") Long employeeId);
    boolean existsByEmployeeIdAndStatusIn(Long employeeId, Collection<BackgroundCheckStatus> statuses);
}
