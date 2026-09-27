package com.bitcomputer.employeeportal.backgroundcheck;

import com.bitcomputer.employeeportal.backgroundcheck.client.ExternalBackgroundCheck;

import com.bitcomputer.employeeportal.backgroundcheck.client.BackgroundCheckClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.bitcomputer.employeeportal.employee.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BackgroundCheckRetryTest {
    BackgroundCheckRepository repository = mock(BackgroundCheckRepository.class);
    BackgroundCheckClient client = mock(BackgroundCheckClient.class);
    BackgroundCheckService service = new BackgroundCheckService(mock(EmployeeRepository.class), repository, client);

    BackgroundCheck check() {
        Employee employee = new Employee("EMP-003", "남궁", "서준", LocalDate.of(1988,7,21), EmploymentStatus.ACTIVE);
        ReflectionTestUtils.setField(employee,"id",3L);
        BackgroundCheck c = new BackgroundCheck(employee,Instant.now());
        ReflectionTestUtils.setField(c,"id",1L);
        c.submitted(new ExternalBackgroundCheck("CHK-existing","EMP-003","pending",null,null,null,null,null),Instant.now());
        when(repository.findByIdAndEmployeeId(1L,3L)).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        return c;
    }

    @Test void repeatedErrorsKeepOriginalDeadlineAndRespectServerDelay() {
        BackgroundCheck c=check();
        Instant first=Instant.parse("2026-09-27T00:00:00Z");
        c.retryAfter(first,Duration.ofSeconds(30),Duration.ofSeconds(180));
        assertEquals(first.plusSeconds(30),c.getNextPollAt());
        for(int i=1;i<=10;i++)
            c.retryAfter(first.plusSeconds(i*5),Duration.ofSeconds(5),Duration.ofSeconds(180));
        assertEquals(first.plusSeconds(180),c.getRetryDeadlineAt());
        c.retryAfter(first.plusSeconds(170),Duration.ofSeconds(30),Duration.ofSeconds(180));
        assertEquals(c.getRetryDeadlineAt(),c.getNextPollAt());
    }

    @Test void deadlineStopsWithoutAnotherGet() throws Exception {
        BackgroundCheck c=check();
        c.retryAfter(Instant.now().minusSeconds(181),Duration.ofSeconds(5),Duration.ofSeconds(180));
        service.refresh(3L,1L,null);
        assertEquals("RETRY_TIME_EXHAUSTED",c.getTrackingStopReason());
        assertEquals(BackgroundCheckStatus.PENDING,c.getStatus());
        assertNull(c.getNextPollAt());
        verifyNoInteractions(client);
    }

    @Test void http503SchedulesWithoutSleepingAndSuccessResetsBudget() throws Exception {
        BackgroundCheck c=check();
        when(client.get(eq(1L),eq(3L),eq("CHK-existing"),any()))
                .thenThrow(new BackgroundCheckClient.ExternalHttpException(503,Duration.ofSeconds(30)))
                .thenReturn(new ExternalBackgroundCheck("CHK-existing","EMP-003","pending",null,null,null,null,null));
        service.refresh(3L,1L,null);
        assertNotNull(c.getRetryDeadlineAt());
        assertNotNull(c.getNextPollAt());
        service.refresh(3L,1L,null);
        assertNull(c.getRetryDeadlineAt());
        assertNull(c.getTrackingStopReason());
        assertNotNull(c.getNextPollAt());
    }

    @Test void manualRetryUsesSameIdAndIsIdempotent() throws Exception {
        BackgroundCheck c=check();
        c.stopTracking("RETRY_TIME_EXHAUSTED");
        when(repository.findForUpdate(1L,3L)).thenReturn(Optional.of(c));
        service.retry(3L,1L,2L);
        Instant scheduled=c.getNextPollAt();
        service.retry(3L,1L,2L);
        assertEquals(scheduled,c.getNextPollAt());
        assertEquals("CHK-existing",c.getExternalCheckId());
        assertNull(c.getTrackingStopReason());
        when(client.get(eq(1L),eq(3L),eq("CHK-existing"),any()))
                .thenReturn(new ExternalBackgroundCheck("CHK-existing","EMP-003","clear",false,true,true,"good",Instant.now()));
        service.refresh(3L,1L,null);
        verify(client).get(eq(1L),eq(3L),eq("CHK-existing"),any());
        verify(client,never()).create(any(),any(),any(),any(),any(),any());
        assertEquals(BackgroundCheckStatus.CLEAR,c.getStatus());
        assertNull(c.getNextPollAt());
    }
}
