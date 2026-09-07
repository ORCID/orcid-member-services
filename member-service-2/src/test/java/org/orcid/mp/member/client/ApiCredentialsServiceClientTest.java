package org.orcid.mp.member.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ApiCredentialsServiceClientTest {

    private ApiCredentialsServiceClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // Build a RestClient backed by a Mock Server instead of Mockito
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new ApiCredentialsServiceClient();

        // Inject the dependencies using Spring's ReflectionTestUtils
        ReflectionTestUtils.setField(client, "restClient", restClient);
        ReflectionTestUtils.setField(client, "apiBaseUrl", "http://localhost:8080/api");
        ReflectionTestUtils.setField(client, "clientId", "test-client");
        ReflectionTestUtils.setField(client, "clientSecret", "test-secret");
    }

    @Test
    void shouldFetchTokenAndDataSuccessfully() {
        // 1. Expect Token Request
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\": \"token-123\"}", MediaType.APPLICATION_JSON));

        // 2. Expect Data Request with the acquired token
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/member-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andRespond(withSuccess("{\"clients\": []}", MediaType.APPLICATION_JSON));

        // Execute
        String result = client.getOrcidRegistryApiClients("member-1");

        // Verify
        assertEquals("{\"clients\": []}", result);
        mockServer.verify();
    }

    @Test
    void shouldRefreshTokenAndRetryOn401() {
        // Inject an explicitly expired/invalid token to trigger the 401
        @SuppressWarnings("unchecked")
        AtomicReference<String> tokenRef = (AtomicReference<String>) ReflectionTestUtils.getField(client, "accessToken");
        assertNotNull(tokenRef);
        tokenRef.set("expired-token");

        // 1. Data Request fails with 401 Unauthorized
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/member-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer expired-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // 2. Catch block triggers Token Refresh
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\": \"brand-new-token\"}", MediaType.APPLICATION_JSON));

        // 3. Retry Data Request with the NEW token
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/member-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer brand-new-token"))
                .andRespond(withSuccess("{\"clients\": [\"test-client\"]}", MediaType.APPLICATION_JSON));

        // Execute
        String result = client.getOrcidRegistryApiClients("member-1");

        // Verify
        assertEquals("{\"clients\": [\"test-client\"]}", result);
        mockServer.verify();
    }

    @Test
    void shouldReturnNullOnNon2xxResponseExcept401And403() {
        // Inject an active token
        @SuppressWarnings("unchecked")
        AtomicReference<String> tokenRef = (AtomicReference<String>) ReflectionTestUtils.getField(client, "accessToken");
        assertNotNull(tokenRef);
        tokenRef.set("active-token");

        // Request returns 404 Not Found (should be caught and return null)
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/member-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not Found"));

        // Execute
        String result = client.getOrcidRegistryApiClients("member-1");

        // Verify
        assertNull(result, "Expected null response for 404 without throwing exception");
        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionIfTokenFetchFails() {
        // Token endpoint is broken (e.g. 500 Server Error or Bad Credentials)
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Execute & Verify Exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            client.getOrcidRegistryApiClients("member-1");
        });

        assertTrue(exception.getMessage().contains("Error while acquiring access token"));
        mockServer.verify();
    }
}