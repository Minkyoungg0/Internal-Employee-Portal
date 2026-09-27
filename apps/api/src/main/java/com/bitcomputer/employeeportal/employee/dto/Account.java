package com.bitcomputer.employeeportal.employee.dto;

import com.bitcomputer.employeeportal.auth.AccountRole;

public record Account(String username, AccountRole role, boolean enabled, boolean passwordChangeRequired) {}
