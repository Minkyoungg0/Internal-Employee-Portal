ALTER TABLE employee
    ADD COLUMN last_name VARCHAR(50) NULL AFTER employee_number,
    ADD COLUMN first_name VARCHAR(50) NULL AFTER last_name,
    ADD COLUMN termination_date DATE NULL AFTER employment_status,
    ADD COLUMN terminated_at TIMESTAMP(6) NULL AFTER termination_date;

UPDATE employee SET
    last_name = '관',
    first_name = '리자'
WHERE employee_number = 'EMP-000';

ALTER TABLE employee
    MODIFY COLUMN last_name VARCHAR(50) NOT NULL,
    MODIFY COLUMN first_name VARCHAR(50) NOT NULL,
    DROP COLUMN background_check_first_name,
    DROP COLUMN background_check_last_name;

ALTER TABLE employee_account
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT TRUE AFTER enabled;

CREATE TABLE background_check (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    external_check_id VARCHAR(100) NULL UNIQUE,
    submitted_first_name VARCHAR(50) NOT NULL,
    submitted_last_name VARCHAR(50) NOT NULL,
    submitted_date_of_birth DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    criminal_record BOOLEAN NULL,
    education_verified BOOLEAN NULL,
    employment_verified BOOLEAN NULL,
    credit_score VARCHAR(20) NULL,
    requested_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    last_checked_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_background_check_employee FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT background_check_status_check CHECK (
        status IN ('REQUESTING', 'PENDING', 'CLEAR', 'FLAGGED', 'SUBMISSION_UNKNOWN', 'SUBMISSION_FAILED')
    ),
    INDEX idx_background_check_employee_requested (employee_id, requested_at)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Background Check 요청과 필요한 결과 필드';
