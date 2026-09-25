package com.bitcomputer.employeeportal.backgroundcheck;

import java.time.Instant;

public record ExternalBackgroundCheck(String checkId, String employeeId, String status,
                                      Boolean criminalRecord, Boolean educationVerified,
                                      Boolean employmentVerified, String creditScore,
                                      Instant completedAt) {}
