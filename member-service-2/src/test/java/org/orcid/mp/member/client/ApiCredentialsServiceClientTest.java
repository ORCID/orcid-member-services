package org.orcid.mp.member.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
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

        ReflectionTestUtils.setField(client, "restClient", restClient);
        ReflectionTestUtils.setField(client, "apiBaseUrl", "http://localhost:8080/api");
        ReflectionTestUtils.setField(client, "clientId", "test-client");
        ReflectionTestUtils.setField(client, "clientSecret", "test-secret");
    }

    @Test
    void getApiClientsForMember_ShouldFetchTokenAndDataSuccessfully() {
        // 1. Expect Token Request
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\": \"token-123\"}", MediaType.APPLICATION_JSON));

        // 2. Expect Data Request with the acquired token
        String expectedUrl = "http://localhost:8080/api/client-details?memberId=member-1&size=100";
        String mockResponseJson = "{\"content\": [{\"clientDetailsId\": \"APP-1\", \"clientName\": \"Test\"}], \"totalElements\": 1, \"totalPages\": 1}";

        mockServer.expect(ExpectedCount.once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        // Execute
        ApiClientSummaryPage result = client.getApiClientsForMember("member-1");

        // Verify
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("APP-1", result.getContent().get(0).getClientDetailsId());
        assertEquals(1, result.getTotalElements());
        mockServer.verify();
    }

    @Test
    void getApiClientDetails_ShouldFetchDataSuccessfully() {
        // Inject an active token directly
        setMockAccessToken("active-token");

        // Expect Data Request
        String expectedUrl = "http://localhost:8080/api/client-details/APP-123";
        String mockResponseJson = "{\"clientDetailsId\": \"APP-123\", \"clientName\": \"Detailed Client\"}";

        mockServer.expect(ExpectedCount.once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer active-token"))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        // Execute
        ApiClientDetails result = client.getApiClientDetails("APP-123");

        // Verify
        assertNotNull(result);
        assertEquals("APP-123", result.getClientDetailsId());
        assertEquals("Detailed Client", result.getClientName());
        mockServer.verify();
    }

    @Test
    void shouldRefreshTokenAndRetryOn401() {
        // Inject an explicitly expired/invalid token to trigger the 401
        setMockAccessToken("expired-token");

        String expectedUrl = "http://localhost:8080/api/client-details/APP-123";

        // 1. Data Request fails with 401 Unauthorized
        mockServer.expect(ExpectedCount.once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer expired-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // 2. Catch block triggers Token Refresh
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\": \"brand-new-token\"}", MediaType.APPLICATION_JSON));

        // 3. Retry Data Request with the NEW token
        mockServer.expect(ExpectedCount.once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer brand-new-token"))
                .andRespond(withSuccess("{\"clientDetailsId\": \"APP-123\"}", MediaType.APPLICATION_JSON));

        // Execute
        ApiClientDetails result = client.getApiClientDetails("APP-123");

        // Verify
        assertNotNull(result);
        assertEquals("APP-123", result.getClientDetailsId());
        mockServer.verify();
    }

    @Test
    void getApiClientDetails_ShouldReturnNullOn404() {
        setMockAccessToken("active-token");

        // Request returns 404 Not Found (should be caught and return null)
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-MISSING"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not Found"));

        // Execute
        ApiClientDetails result = client.getApiClientDetails("APP-MISSING");

        // Verify
        assertNull(result, "Expected null response for 404 without throwing exception");
        mockServer.verify();
    }

    @Test
    void getApiClientsForMember_ShouldReturnNullOn404() {
        setMockAccessToken("active-token");

        // Request returns 404 Not Found
        String expectedUrl = "http://localhost:8080/api/client-details?memberId=member-missing&size=100";
        mockServer.expect(ExpectedCount.once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not Found"));

        // Execute
        ApiClientSummaryPage result = client.getApiClientsForMember("member-missing");

        // Verify - the client handles the 404 by catching the exception and returning null
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
            client.getApiClientsForMember("member-1");
        });

        assertTrue(exception.getMessage().contains("Error while acquiring access token"));
        mockServer.verify();
    }

    // Helper to inject a token directly, avoiding the token-fetch flow for isolated tests
    private void setMockAccessToken(String tokenValue) {
        @SuppressWarnings("unchecked")
        AtomicReference<String> tokenRef = (AtomicReference<String>) ReflectionTestUtils.getField(client, "accessToken");
        assertNotNull(tokenRef);
        tokenRef.set(tokenValue);
    }
}