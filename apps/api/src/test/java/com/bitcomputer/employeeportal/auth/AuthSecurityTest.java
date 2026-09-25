package com.bitcomputer.employeeportal.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bitcomputer.employeeportal.config.SecurityConfig;
import com.bitcomputer.employeeportal.employee.Employee;
import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CurrentAccountStatusFilter.class})
class AuthSecurityTest {
    private static final String PASSWORD = "Employee!234";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    EmployeeAccountRepository accountRepository;

    private Employee employee;
    private EmployeeAccount account;

    @BeforeEach
    void setUp() {
        employee = new Employee("EMP-001", "김민준", LocalDate.of(1990, 3, 15), EmploymentStatus.ACTIVE);
        ReflectionTestUtils.setField(employee, "id", 1L);
        account = new EmployeeAccount(
                employee,
                "employee",
                passwordEncoder.encode(PASSWORD),
                AccountRole.EMPLOYEE,
                true
        );
        ReflectionTestUtils.setField(account, "id", 10L);
        ReflectionTestUtils.setField(account, "passwordChangeRequired", false);
        when(accountRepository.findWithEmployeeByUsername("employee")).thenReturn(Optional.of(account));
        when(accountRepository.findWithEmployeeById(10L)).thenReturn(Optional.of(account));
    }

    @Test
    void issuesCsrfTokenBeforeLogin() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void logsInAndRestoresCurrentUserFromSession() throws Exception {
        MockHttpSession session = login();

        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        PortalPrincipal storedPrincipal = (PortalPrincipal) context.getAuthentication().getPrincipal();
        assertNull(storedPrincipal.getPassword());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void rejectsLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "employee")
                        .param("password", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsSameFailureForInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "employee")
                        .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void employeeCannotUseAdminApi() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/admin/employees").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void initialPasswordMustBeChangedBeforeUsingProtectedFeatures() throws Exception {
        ReflectionTestUtils.setField(account, "passwordChangeRequired", true);
        MockHttpSession session = login();

        mockMvc.perform(get("/api/me/profile").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void adminIsNotForcedToChangeInitialPassword() throws Exception {
        account = new EmployeeAccount(
                employee,
                "admin",
                passwordEncoder.encode(PASSWORD),
                AccountRole.ADMIN,
                true
        );
        ReflectionTestUtils.setField(account, "id", 20L);
        ReflectionTestUtils.setField(account, "passwordChangeRequired", true);
        when(accountRepository.findWithEmployeeByUsername("admin")).thenReturn(Optional.of(account));
        when(accountRepository.findWithEmployeeById(20L)).thenReturn(Optional.of(account));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/api/admin/employees").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void blocksExistingSessionAfterEmployeeIsTerminated() throws Exception {
        MockHttpSession session = login();
        employee.terminate();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seededBcryptHashesMatchDocumentedDemoPasswords() {
        assertTrue(passwordEncoder.matches(
                "Admin!234",
                "$2y$10$YrKGKTS44TsyjffWLYsPeelWOsF5glria53n0HlZem.Bjtg55fB5a"
        ));
        assertTrue(passwordEncoder.matches(
                PASSWORD,
                "$2y$10$Hdw6ekz.cfpTE4.s1PU4k.uAkvf8AYyixX1UoZQ65i9Lvuuf1ZPVe"
        ));
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "employee")
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
