INSERT INTO employee_account (employee_id, username, password_hash, role)
SELECT id, 'employee', '$2y$10$Hdw6ekz.cfpTE4.s1PU4k.uAkvf8AYyixX1UoZQ65i9Lvuuf1ZPVe', 'EMPLOYEE'
FROM employee
WHERE employee_number = 'EMP-001';

INSERT INTO employee_account (employee_id, username, password_hash, role)
SELECT id, 'admin', '$2y$10$YrKGKTS44TsyjffWLYsPeelWOsF5glria53n0HlZem.Bjtg55fB5a', 'ADMIN'
FROM employee
WHERE employee_number = 'EMP-010';
