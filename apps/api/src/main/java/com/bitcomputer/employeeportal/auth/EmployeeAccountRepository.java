package com.bitcomputer.employeeportal.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeAccountRepository extends JpaRepository<EmployeeAccount, Long> {
    boolean existsByUsername(String username);

    Optional<EmployeeAccount> findByEmployeeId(Long employeeId);
    @Query("select account from EmployeeAccount account join fetch account.employee where account.username = :username")
    Optional<EmployeeAccount> findWithEmployeeByUsername(@Param("username") String username);

    @Query("select account from EmployeeAccount account join fetch account.employee where account.id = :id")
    Optional<EmployeeAccount> findWithEmployeeById(@Param("id") Long id);
}
