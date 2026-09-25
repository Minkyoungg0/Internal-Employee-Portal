package com.bitcomputer.employeeportal.backgroundcheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BackgroundCheckClient {
    private final String baseUrl;
    private final String candidateKey;
    private final ObjectMapper objectMapper;
    private final Duration getTimeout;
    private final int getMaxAttempts;
    private final Duration retry500Delay;
    private final Duration retry503DefaultDelay;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public BackgroundCheckClient(@Value("${background-check.base-url}") String baseUrl,
                                 @Value("${background-check.candidate-key}") String candidateKey,
                                 @Value("${background-check.get-timeout}") Duration getTimeout,
                                 @Value("${background-check.get-max-attempts}") int getMaxAttempts,
                                 @Value("${background-check.retry-500-delay}") Duration retry500Delay,
                                 @Value("${background-check.retry-503-default-delay}") Duration retry503DefaultDelay,
                                 ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.candidateKey = candidateKey;
        this.objectMapper = objectMapper;
        this.getTimeout = getTimeout;
        this.getMaxAttempts = getMaxAttempts;
        this.retry500Delay = retry500Delay;
        this.retry503DefaultDelay = retry503DefaultDelay;
    }

    public ExternalBackgroundCheck create(String employeeNumber, String firstName, String lastName,
                                          java.time.LocalDate dateOfBirth) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(new CreateRequest(employeeNumber, firstName, lastName, dateOfBirth));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/background-checks"))
                .timeout(Duration.ofSeconds(35)) // POST 지연 표본을 별도로 확보할 때 설정값을 재검토한다.
                .header("Content-Type", "application/json")
                .header("X-Candidate-Key", candidateKey)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new ExternalHttpException(response.statusCode(), response.body());
        return parse(response.body());
    }

    public ExternalBackgroundCheck get(String checkId) throws IOException, InterruptedException {
        int attempts = 0;
        while (true) {
            attempts++;
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/background-checks/" +
                            URLEncoder.encode(checkId, StandardCharsets.UTF_8)))
                    .timeout(getTimeout).header("X-Candidate-Key", candidateKey).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return parse(response.body());
            if ((response.statusCode() != 500 && response.statusCode() != 503) || attempts >= getMaxAttempts)
                throw new ExternalHttpException(response.statusCode(), response.body());
            Thread.sleep(response.statusCode() == 500 ? retry500Delay.toMillis() : retryAfter(response).toMillis());
        }
    }

    private Duration retryAfter(HttpResponse<String> response) {
        String header = response.headers().firstValue("Retry-After").orElse(null);
        if (header != null) try { return Duration.ofSeconds(Integer.parseInt(header)); } catch (NumberFormatException ignored) {}
        try {
            int bodyValue = objectMapper.readTree(response.body()).path("retryAfter").asInt(0);
            if (bodyValue > 0) return Duration.ofSeconds(bodyValue);
        } catch (Exception ignored) {}
        return retry503DefaultDelay;
    }

    private ExternalBackgroundCheck parse(String body) throws IOException {
        JsonNode node = objectMapper.readTree(body);
        return new ExternalBackgroundCheck(text(node, "checkId"), text(node, "employeeId"), text(node, "status"),
                nullableBoolean(node, "criminalRecord"), nullableBoolean(node, "educationVerified"),
                nullableBoolean(node, "employmentVerified"), nullableText(node, "creditScore"),
                nullableInstant(node, "completedAt"));
    }

    private String text(JsonNode node, String field) throws IOException {
        String value = nullableText(node, field);
        if (value == null || value.isBlank()) throw new IOException("Background Check 응답에 " + field + "가 없습니다.");
        return value;
    }
    private String nullableText(JsonNode node, String field) { return node.path(field).isTextual() ? node.path(field).asText() : null; }
    private Boolean nullableBoolean(JsonNode node, String field) { return node.path(field).isBoolean() ? node.path(field).asBoolean() : null; }
    private Instant nullableInstant(JsonNode node, String field) {
        String value = nullableText(node, field);
        try { return value == null ? null : Instant.parse(value); } catch (Exception ignored) { return null; }
    }

    private record CreateRequest(String employeeId, String firstName, String lastName, java.time.LocalDate dateOfBirth) {}

    public static class ExternalHttpException extends IOException {
        private final int status;
        ExternalHttpException(int status, String ignoredBody) { super("Background Check API HTTP " + status); this.status = status; }
        public int getStatus() { return status; }
    }
}
