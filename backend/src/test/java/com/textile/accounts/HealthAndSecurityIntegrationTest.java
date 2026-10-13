package com.textile.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthAndSecurityIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("GET /api/v1/health returns HTTP 200 with status UP")
    void publicHealthEndpointReturns200() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/v1/health")))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("GET /actuator/health returns HTTP 200 with status UP and no details")
    void actuatorHealthReturns200WithoutDetails() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/actuator/health")))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        assertThat(response.body()).doesNotContain("details");
    }

    @Test
    @DisplayName("Unauthenticated request to any other /api/** path is rejected with HTTP 401")
    void otherApiEndpointsAreRejectedByDefault() throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/v1/customers")))
            .GET()
            .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(getResponse.statusCode()).isEqualTo(401);

        HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/v1/invoices")))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(postResponse.statusCode()).isIn(401, 403);
    }
}
