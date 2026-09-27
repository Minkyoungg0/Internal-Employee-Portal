package com.bitcomputer.employeeportal.auth;

import com.bitcomputer.employeeportal.auth.dto.ChangePasswordRequest;
import com.bitcomputer.employeeportal.employee.*;
import com.bitcomputer.employeeportal.common.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordServiceTest {
    @Test
    void incorrectPasswordDoesNotChangeStoredPassword() {
        var accounts = mock(EmployeeAccountRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var account = new EmployeeAccount(new Employee("EMP-001", "김", "민준", null,
                EmploymentStatus.ACTIVE), "employee", "old-hash", AccountRole.EMPLOYEE, true);
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        assertThatThrownBy(() -> new PasswordService(accounts, encoder).changePassword(1L,
                new ChangePasswordRequest("wrong", "new-password"))).isInstanceOf(ApiException.class);
        assertThat(account.getPasswordHash()).isEqualTo("old-hash");
        assertThat(account.isPasswordChangeRequired()).isTrue();
        verify(encoder, never()).encode(any());
    }

    @Test
    void successfulChangeEncodesPasswordAndClearsInitialChangeFlag() {
        var accounts = mock(EmployeeAccountRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var account = new EmployeeAccount(new Employee("EMP-001", "김", "민준", null,
                EmploymentStatus.ACTIVE), "employee", "old-hash", AccountRole.EMPLOYEE, true);
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);
        when(encoder.encode("new-password")).thenReturn("new-hash");
        new PasswordService(accounts, encoder).changePassword(1L,
                new ChangePasswordRequest("old-password", "new-password"));
        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(account.isPasswordChangeRequired()).isFalse();
    }
}
