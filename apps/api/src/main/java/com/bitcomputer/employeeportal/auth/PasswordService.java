package com.bitcomputer.employeeportal.auth;

import com.bitcomputer.employeeportal.auth.dto.ChangePasswordRequest;
import com.bitcomputer.employeeportal.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {
    private final EmployeeAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(EmployeeAccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(Long accountId, ChangePasswordRequest request) {
        EmployeeAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED", "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
        account.changePassword(passwordEncoder.encode(request.newPassword()));
    }
}
