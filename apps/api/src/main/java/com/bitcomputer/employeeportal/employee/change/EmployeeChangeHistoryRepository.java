package com.bitcomputer.employeeportal.employee.change;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeChangeHistoryRepository extends JpaRepository<EmployeeChangeHistory, Long> {
    boolean existsByEmployeeIdAndStatus(Long employeeId, EmployeeChangeStatus status);
    @Query("""
            select history from EmployeeChangeHistory history
            join fetch history.employee
            where history.employee.id = :employeeId
            order by history.requestedAt desc
            """)
    List<EmployeeChangeHistory> findAllByEmployeeIdWithEmployee(@Param("employeeId") Long employeeId);

    @Query("""
            select history from EmployeeChangeHistory history
            join fetch history.employee
            order by history.requestedAt desc
            """)
    List<EmployeeChangeHistory> findAllWithEmployee();

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select history from EmployeeChangeHistory history join fetch history.employee where history.id = :id")
    Optional<EmployeeChangeHistory> findByIdForUpdate(@Param("id") Long id);
}
