package com.bitcomputer.employeeportal.backgroundcheck;

public enum BackgroundCheckStatus {
    REQUESTING, PENDING, CLEAR, FLAGGED, SUBMISSION_UNKNOWN, SUBMISSION_FAILED;

    public boolean blocksNewRequest() {
        return this == REQUESTING || this == PENDING || this == SUBMISSION_UNKNOWN;
    }

    public boolean isFinal() { return this == CLEAR || this == FLAGGED; }
}
