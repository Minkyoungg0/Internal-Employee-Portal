package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.backgroundcheck.client.*;
import com.bitcomputer.employeeportal.common.ApiException;
import com.bitcomputer.employeeportal.employee.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackgroundCheckRetentionTest {
    Employee employee() {
        Employee e = new Employee("EMP-001", "김", "민준", LocalDate.of(1990,3,15), EmploymentStatus.ACTIVE);
        ReflectionTestUtils.setField(e, "id", 1L);
        return e;
    }
    ExternalBackgroundCheck completed(Instant at) {
        return new ExternalBackgroundCheck("CHK-1", "EMP-001", "clear", false, true, true, "good", at);
    }

    @Test void completionStartsRetentionOnceAndRetryDoesNotExtendIt() {
        Instant requested = Instant.parse("2026-01-01T00:00:00Z");
        BackgroundCheck c = new BackgroundCheck(employee(), requested);
        assertEquals(requested.atZone(ZoneId.of("Asia/Seoul")).toLocalDate().plusDays(90).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(), c.getExpiresAt());
        Instant completed = requested.plusSeconds(50);
        c.submitted(completed(completed), completed.plusSeconds(20));
        Instant expiry = Instant.parse("2026-03-31T15:00:00Z");
        assertEquals(expiry, c.getExpiresAt());
        c.resumeTracking(completed.plusSeconds(40));
        c.apply(completed(null), completed.plusSeconds(60));
        assertEquals(expiry, c.getExpiresAt());
        assertFalse(c.isExpired(expiry.minusNanos(1)));
        assertTrue(c.isExpired(expiry));
    }

    @Test void expiryUsesKoreanDateRegardlessOfCompletionTime() {
        BackgroundCheck morning = new BackgroundCheck(employee(), Instant.parse("2026-01-01T15:00:00Z"));
        BackgroundCheck evening = new BackgroundCheck(employee(), Instant.parse("2026-01-02T14:59:59Z"));
        assertEquals(morning.getExpiresAt(), evening.getExpiresAt());
        BackgroundCheck nextDay = new BackgroundCheck(employee(), Instant.parse("2026-01-02T15:00:00Z"));
        assertEquals(morning.getExpiresAt().plus(1, ChronoUnit.DAYS), nextDay.getExpiresAt());
    }

    @Test void expiredPendingCannotCallExternalApiOrRetry() throws Exception {
        var employees = mock(EmployeeRepository.class);
        var repo = mock(BackgroundCheckRepository.class);
        var client = mock(BackgroundCheckClient.class);
        var service = new BackgroundCheckService(employees, repo, client, mock(PlatformTransactionManager.class));
        Employee e = employee();
        var c = new BackgroundCheck(e, Instant.now().minus(91, ChronoUnit.DAYS));
        when(repo.findByIdAndEmployeeId(1L,1L)).thenReturn(Optional.of(c));
        when(repo.findForUpdate(1L,1L)).thenReturn(Optional.of(c));
        when(employees.findByIdForUpdate(1L)).thenReturn(Optional.of(e));
        assertEquals("BACKGROUND_CHECK_EXPIRED", assertThrows(ApiException.class, () -> service.refresh(1L,1L,null)).getCode());
        assertThrows(ApiException.class, () -> service.retry(1L,1L,null));
        verifyNoInteractions(client);
    }

    @Test void deletedRowDuringGetIsNotRecreated() throws Exception {
        var employees = mock(EmployeeRepository.class);
        var repo = mock(BackgroundCheckRepository.class);
        var client = mock(BackgroundCheckClient.class);
        var service = new BackgroundCheckService(employees, repo, client, mock(PlatformTransactionManager.class));
        Employee e = employee();
        var c = new BackgroundCheck(e, Instant.now());
        ReflectionTestUtils.setField(c, "id", 1L);
        c.submitted(new ExternalBackgroundCheck("CHK-1","EMP-001","pending",null,null,null,null,null), Instant.now());
        when(repo.findByIdAndEmployeeId(1L,1L)).thenReturn(Optional.of(c));
        when(employees.findByIdForUpdate(1L)).thenReturn(Optional.of(e));
        when(client.get(eq(1L),eq(1L),eq("CHK-1"),any())).thenReturn(completed(Instant.now()));
        when(repo.findForUpdate(1L,1L)).thenReturn(Optional.empty());
        assertEquals("BACKGROUND_CHECK_NOT_FOUND", assertThrows(ApiException.class, () -> service.refresh(1L,1L,null)).getCode());
        verify(repo, never()).save(any());
    }

    @Test void terminationDuringPostDiscardsResponse() throws Exception {
        var employees = mock(EmployeeRepository.class);
        var repo = mock(BackgroundCheckRepository.class);
        var client = mock(BackgroundCheckClient.class);
        var service = new BackgroundCheckService(employees, repo, client, mock(PlatformTransactionManager.class));
        Employee e = employee();
        when(employees.findByIdForUpdate(1L)).thenReturn(Optional.of(e));
        when(repo.save(any())).thenAnswer(call -> { BackgroundCheck c = call.getArgument(0); ReflectionTestUtils.setField(c,"id",1L); return c; });
        when(client.create(any(),any(),any(),any(),any(),any())).thenAnswer(call -> {
            e.terminate(LocalDate.now(), Instant.now());
            return completed(Instant.now());
        });
        assertEquals("EMPLOYEE_TERMINATED", assertThrows(ApiException.class, () -> service.start(1L,2L)).getCode());
        verify(repo, times(1)).save(any());
        verify(repo, never()).findForUpdate(any(), any());
    }

    @Test void cleanupDrainsBatchesAndRetriesFailureOnNextRun() {
        var repo = mock(BackgroundCheckRepository.class);
        var cleanup = new BackgroundCheckRetention(repo, mock(PlatformTransactionManager.class));
        when(repo.deleteExpiredBatch(any())).thenReturn(200).thenThrow(new IllegalStateException()).thenReturn(4);
        cleanup.purgeExpired();
        cleanup.purgeExpired();
        verify(repo, times(3)).deleteExpiredBatch(any());
    }
}
