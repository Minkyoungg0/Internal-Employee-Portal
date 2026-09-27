package com.bitcomputer.employeeportal.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank @Pattern(regexp = "EMP-[0-9]{3,}", message = "사번은 EMP-001 형식이어야 합니다.") String employeeNumber,
            @NotBlank @Size(max = 50) String lastName,
            @NotBlank @Size(max = 50) String firstName,
            LocalDate dateOfBirth,
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(min = 8, max = 72, message = "초기 비밀번호는 8자 이상 72자 이하여야 합니다.") String initialPassword
    ) {}
