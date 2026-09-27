package com.bitcomputer.employeeportal.backgroundcheck.dto;

public record Result(Boolean criminalRecord, Boolean educationVerified, Boolean employmentVerified, String creditScore) {}
