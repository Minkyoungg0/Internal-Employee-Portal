package com.bitcomputer.employeeportal.auth;

import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class PortalPrincipal implements UserDetails, CredentialsContainer, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long accountId;
    private final Long employeeId;
    private final String employeeNumber;
    private final String username;
    private final AccountRole role;
    private final boolean accountEnabled;
    private final EmploymentStatus employmentStatus;
    private String password;

    private PortalPrincipal(EmployeeAccount account, String password) {
        this.accountId = account.getId();
        this.employeeId = account.getEmployee().getId();
        this.employeeNumber = account.getEmployee().getEmployeeNumber();
        this.username = account.getUsername();
        this.role = account.getRole();
        this.accountEnabled = account.isEnabled();
        this.employmentStatus = account.getEmployee().getEmploymentStatus();
        this.password = password;
    }

    public static PortalPrincipal forLogin(EmployeeAccount account) {
        return new PortalPrincipal(account, account.getPasswordHash());
    }

    public static PortalPrincipal authenticated(EmployeeAccount account) {
        return new PortalPrincipal(account, null);
    }

    public Long accountId() {
        return accountId;
    }

    public Long employeeId() {
        return employeeId;
    }

    public String employeeNumber() {
        return employeeNumber;
    }

    public AccountRole role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return accountEnabled && employmentStatus == EmploymentStatus.ACTIVE;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PortalPrincipal principal && Objects.equals(accountId, principal.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
}
