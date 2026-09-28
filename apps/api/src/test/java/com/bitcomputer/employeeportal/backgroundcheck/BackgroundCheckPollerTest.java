package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.backgroundcheck.client.ExternalBackgroundCheck;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.bitcomputer.employeeportal.common.ApiException;
import com.bitcomputer.employeeportal.employee.Employee;
import com.bitcomputer.employeeportal.employee.EmploymentStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class BackgroundCheckPollerTest {
    private BackgroundCheck check() {
        Employee e = new Employee("EMP-003", "남궁", "서준", LocalDate.of(1988,7,21), EmploymentStatus.ACTIVE);
        ReflectionTestUtils.setField(e, "id", 3L);
        BackgroundCheck c = new BackgroundCheck(e, Instant.now().minusSeconds(10000));
        ReflectionTestUtils.setField(c, "id", 1L);
        c.submitted(new ExternalBackgroundCheck("CHK-1","EMP-003","pending",null,null,null,null,null), Instant.now());
        return c;
    }

    @Test void pendingHasNoOverallDeadlineAndFinalStopsScheduling() {
        BackgroundCheck c = check();
        Instant now = Instant.now();
        c.apply(new ExternalBackgroundCheck("CHK-1","EMP-003","pending",null,null,null,null,null),now);
        assertEquals(now.plusSeconds(15),c.getNextPollAt());
        c.apply(new ExternalBackgroundCheck("CHK-1","EMP-003","flagged",true,true,true,"good",now),now);
        assertNull(c.getNextPollAt());
        assertEquals(BackgroundCheckStatus.FLAGGED,c.getStatus());
    }

    @Test void removedCheckIsNeverSavedByPoller() {
        BackgroundCheckRepository repo=mock(BackgroundCheckRepository.class);
        BackgroundCheckService service=mock(BackgroundCheckService.class);
        BackgroundCheck c=check();
        when(service.refresh(3L,1L,null)).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY,"BACKGROUND_CHECK_NOT_FOUND","failure"));
        BackgroundCheckPoller poller=new BackgroundCheckPoller(repo,service);
        try {
            poller.poll(c);
            assertNotNull(c.getNextPollAt());
            assertEquals(BackgroundCheckStatus.PENDING,c.getStatus());
            verify(repo, never()).save(any());
        } finally { poller.shutdown(); }
    }

    @Test void repeatedDispatchDoesNotOverlapSameCheck() throws Exception {
        BackgroundCheckRepository repo=mock(BackgroundCheckRepository.class);
        BackgroundCheckService service=mock(BackgroundCheckService.class);
        BackgroundCheck c=check();
        when(repo.findDue(any(),any())).thenReturn(java.util.List.of(c));
        var entered=new java.util.concurrent.CountDownLatch(1);
        var release=new java.util.concurrent.CountDownLatch(1);
        when(service.refresh(3L,1L,null)).thenAnswer(call->{
            entered.countDown();
            release.await(5,java.util.concurrent.TimeUnit.SECONDS);
            return c;
        });
        BackgroundCheckPoller poller=new BackgroundCheckPoller(repo,service);
        try {
            poller.dispatch();
            assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS));
            poller.dispatch();
            verify(service,times(1)).refresh(3L,1L,null);
        } finally {release.countDown();poller.shutdown();}
    }
}
