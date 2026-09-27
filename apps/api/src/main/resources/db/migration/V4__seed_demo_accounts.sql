INSERT INTO employee (employee_number, full_name, date_of_birth) VALUES ('EMP-000', '관리자', NULL);

INSERT INTO employee_account (employee_id, username, password_hash, role)
SELECT id, 'admin', '$2y$10$ck.yfpFwdL/rJGNt9VwuOObKQ9/xv90de.e5JfV5SWtTemK3/28ia', 'ADMIN'
FROM employee
WHERE employee_number = 'EMP-000';
