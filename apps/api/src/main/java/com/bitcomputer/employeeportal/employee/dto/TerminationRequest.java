package com.bitcomputer.employeeportal.employee.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TerminationRequest(@NotNull(message = "실제 퇴사일을 입력해 주세요.") LocalDate terminationDate) {}
