package com.bitcomputer.employeeportal.auth.dto;

public record CsrfResponse(String headerName, String parameterName, String token) {
}
