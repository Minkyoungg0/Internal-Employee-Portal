package com.bitcomputer.employeeportal.employee;

import com.bitcomputer.employeeportal.auth.PortalPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employee-change-requests")
public class AdminEmployeeChangeController {
    private final EmployeeChangeService service;

    public AdminEmployeeChangeController(EmployeeChangeService service) {
        this.service = service;
    }

    @GetMapping
    List<EmployeeChangeResponse> list() {
        return service.listAll().stream().map(EmployeeChangeResponse::from).toList();
    }

    @PostMapping("/{id}/approve")
    EmployeeChangeResponse approve(@PathVariable Long id, @AuthenticationPrincipal PortalPrincipal principal) {
        return EmployeeChangeResponse.from(service.review(id, principal.accountId(), true));
    }

    @PostMapping("/{id}/reject")
    EmployeeChangeResponse reject(@PathVariable Long id, @AuthenticationPrincipal PortalPrincipal principal) {
        return EmployeeChangeResponse.from(service.review(id, principal.accountId(), false));
    }
}
