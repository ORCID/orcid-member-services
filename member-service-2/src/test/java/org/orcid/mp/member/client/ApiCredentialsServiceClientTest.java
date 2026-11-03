package org.orcid.mp.member.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.error.ApiClientValidationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

        String expectedUrl = "http://localhost:8080/api/client-details?memberId=member-1&size=100";
        String mockResponseJson = "{\"content\": [{\"clientDetailsId\": \"APP-1\", \"clientName\": \"Test\"}], \"totalElements\": 1, \"totalPages\": 1}";

        // 2. Expect Data Request with the acquired token
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
    void shouldFetchClientDetailsById() {
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\": \"token-123\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andRespond(withSuccess(
                        "{\"memberId\":\"member-1\",\"clientDetailsId\":\"APP-123\","
                                + "\"clientName\":\"My App\",\"website\":\"https://example.org\","
                                + "\"redirectUris\":[]}",
                        MediaType.APPLICATION_JSON));

        ApiClientDetails result = client.getApiClientDetails("APP-123");

        assertNotNull(result);
        assertEquals("APP-123", result.getClientDetailsId());
        assertEquals("My App", result.getClientName());
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

    @Test
    void update_ShouldPutAndReturnBody() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-123"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer active-token"))
                .andRespond(withSuccess("{\"clientDetailsId\":\"APP-123\",\"clientName\":\"My App\"}", MediaType.APPLICATION_JSON));

        ApiClientDetails request = new ApiClientDetails();
        request.setClientDetailsId("APP-123");
        ApiClientDetails result = client.update("APP-123", request);

        assertEquals("My App", result.getClientName());
        mockServer.verify();
    }

    @Test
    void update_On400_ShouldThrowValidationExceptionWithUpstreamErrorsAndNotRetry() {
        setMockAccessToken("active-token");

        String body = "{\"errors\":[\"Invalid website\"],\"redirectUris\":[{\"redirectUri\":\"x\",\"errors\":[\"Invalid redirect uri\"]}]}";
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-123"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(body));

        ApiClientValidationException ex = assertThrows(ApiClientValidationException.class,
                () -> client.update("APP-123", new ApiClientDetails()));

        assertTrue(ex.getErrors().contains("Invalid website"));
        assertTrue(ex.getErrors().contains("Invalid redirect uri"));
        mockServer.verify();
    }

    @Test
    void update_On400WithoutJsonBody_ShouldUseGenericMessage() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-123"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        ApiClientValidationException ex = assertThrows(ApiClientValidationException.class,
                () -> client.update("APP-123", new ApiClientDetails()));

        assertEquals(1, ex.getErrors().size());
    }

    @Test
    void resetClientSecret_ShouldPostRawClientIdAsTextPlain() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/resetClientSecret"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer active-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("APP-123"))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));

        assertTrue(client.resetClientSecret("APP-123"));
        mockServer.verify();
    }

    @Test
    void resetClientSecret_On500_ShouldNotRetry() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/resetClientSecret"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(RestClientResponseException.class, () -> client.resetClientSecret("APP-123"));
        mockServer.verify();
    }

    @Test
    void getClientSecret_ShouldReturnSecret() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-123/secret"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer active-token"))
                .andRespond(withSuccess("{\"clientSecret\":\"s3cret\"}", MediaType.APPLICATION_JSON));

        assertEquals("s3cret", client.getClientSecret("APP-123"));
        mockServer.verify();
    }

    @Test
    void getClientSecret_On404_ShouldReturnNull() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-404/secret"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertNull(client.getClientSecret("APP-404"));
        mockServer.verify();
    }

    @Test
    void getClientSecret_WhenResponseHasNoSecret_ShouldReturnNull() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-123/secret"))
                .andRespond(withSuccess("{\"clientSecret\":null}", MediaType.APPLICATION_JSON));

        assertNull(client.getClientSecret("APP-123"));
        mockServer.verify();
    }

    @Test
    void getClientSecret_On401_ShouldRefreshTokenAndRetry() {
        setMockAccessToken("expired-token");

        String url = "http://localhost:8080/api/client-details/APP-123/secret";
        mockServer.expect(ExpectedCount.once(), requestTo(url))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"new-token\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(ExpectedCount.once(), requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer new-token"))
                .andRespond(withSuccess("{\"clientSecret\":\"s3cret\"}", MediaType.APPLICATION_JSON));

        assertEquals("s3cret", client.getClientSecret("APP-123"));
        mockServer.verify();
    }

    @Test
    void getClientSecret_On500_ShouldPropagateInsteadOfReturningNull() {
        setMockAccessToken("active-token");

        mockServer.expect(ExpectedCount.once(), requestTo("http://localhost:8080/api/client-details/APP-123/secret"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(RestClientResponseException.class, () -> client.getClientSecret("APP-123"));
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
