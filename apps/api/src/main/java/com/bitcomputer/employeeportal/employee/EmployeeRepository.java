package com.bitcomputer.employeeportal.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmployeeNumber(String employeeNumber);
    @Query("select employee from Employee employee "
            + "where not exists (select account.id from EmployeeAccount account "
            + "where account.employee = employee "
            + "and account.role = com.bitcomputer.employeeportal.auth.AccountRole.ADMIN) "
            + "order by employee.employeeNumber asc")
    java.util.List<Employee> findAllNonAdminEmployeesOrderByEmployeeNumberAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select employee from Employee employee where employee.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);
}
