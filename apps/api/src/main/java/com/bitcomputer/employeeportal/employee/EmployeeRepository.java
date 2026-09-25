package com.bitcomputer.employeeportal.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmployeeNumber(String employeeNumber);
    java.util.List<Employee> findAllByOrderByEmployeeNumberAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select employee from Employee employee where employee.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);
}
