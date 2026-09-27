CREATE TABLE employee_change_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    requested_by_account_id BIGINT NOT NULL,
    reviewed_by_account_id BIGINT,
    previous_last_name VARCHAR(50) NOT NULL,
    previous_first_name VARCHAR(50) NOT NULL,
    previous_date_of_birth DATE,
    requested_last_name VARCHAR(50) NOT NULL,
    requested_first_name VARCHAR(50) NOT NULL,
    requested_date_of_birth DATE,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP(6) NOT NULL,
    reviewed_at TIMESTAMP(6),
    CONSTRAINT fk_employee_change_history_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_employee_change_history_requester
        FOREIGN KEY (requested_by_account_id) REFERENCES employee_account(id),
    CONSTRAINT fk_employee_change_history_reviewer
        FOREIGN KEY (reviewed_by_account_id) REFERENCES employee_account(id),
    CONSTRAINT employee_change_history_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    INDEX idx_employee_change_history_employee_requested
        (employee_id, requested_at DESC),
    INDEX idx_employee_change_history_status_requested
        (status, requested_at)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='직원 인적사항 변경 요청 및 승인 이력';
