package com.bitcomputer.employeeportal.auth.dto;

import com.bitcomputer.employeeportal.auth.AccountRole;
import com.bitcomputer.employeeportal.auth.PortalPrincipal;

public record CurrentUserResponse(
        Long accountId,
        Long employeeId,
        String employeeNumber,
        String username,
        AccountRole role,
        boolean passwordChangeRequired
) {
    public static CurrentUserResponse from(PortalPrincipal principal) {
        return new CurrentUserResponse(
                principal.accountId(),
                principal.employeeId(),
                principal.employeeNumber(),
                principal.getUsername(),
                principal.role(),
                principal.passwordChangeRequired()
        );
    }
}
