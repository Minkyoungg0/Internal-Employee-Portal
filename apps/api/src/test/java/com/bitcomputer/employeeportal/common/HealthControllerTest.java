package com.bitcomputer.employeeportal.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.bitcomputer.employeeportal.config.SecurityConfig;
import com.bitcomputer.employeeportal.auth.CurrentAccountStatusFilter;
import com.bitcomputer.employeeportal.auth.EmployeeAccountRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, CurrentAccountStatusFilter.class})
class HealthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmployeeAccountRepository accountRepository;

    @Test
    void returnsHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
