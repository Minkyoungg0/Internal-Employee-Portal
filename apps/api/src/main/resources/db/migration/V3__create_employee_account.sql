CREATE TABLE employee_account (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_employee_account_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT employee_account_role_check
        CHECK (role IN ('EMPLOYEE', 'ADMIN'))
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='직원 포털 로그인 계정';
