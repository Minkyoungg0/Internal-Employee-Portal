package com.bitcomputer.employeeportal.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "상태 확인")
public class HealthController {
    @GetMapping
    @Operation(summary = "애플리케이션 상태 확인", description = "API 서버가 요청을 처리할 수 있는지 확인합니다.")
    Map<String, Object> health() {
        return Map.of("status", "ok", "timestamp", Instant.now().toString());
    }
}
