package org.orcid.mp.member.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.salesforce.MemberUpdateData;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class SalesforceClientTest {

    @InjectMocks
    private SalesforceClient salesforceClient;

    private MockRestServiceServer mockServer;

    private final String MOCK_TOKEN = "mock-access-token-123";
    private final String BASE_URL = "https://api.salesforce.mock";
    private final String LOGIN_URL = "https://login.salesforce.mock";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        ReflectionTestUtils.setField(salesforceClient, "accessToken", new AtomicReference<>());
        ReflectionTestUtils.setField(salesforceClient, "restClient", restClient);
        ReflectionTestUtils.setField(salesforceClient, "salesforceAPIBaseUrl", BASE_URL);
        ReflectionTestUtils.setField(salesforceClient, "username", "test-user");
        ReflectionTestUtils.setField(salesforceClient, "password", "test-pass");
        ReflectionTestUtils.setField(salesforceClient, "clientId", "test-client-id");
        ReflectionTestUtils.setField(salesforceClient, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(salesforceClient, "loginUrl", LOGIN_URL);
    }

    // ==========================================
    // SUCCESSFUL GET METHOD TESTS
    // ==========================================

    @Test
    void getMemberDetails_shouldReturnData() {
        expectTokenRequest();
        expectQueryRequest("SF_123", "{\"records\":[{\"Name\":\"Test Member\"}]}");

        String result = salesforceClient.getMemberDetails("SF_123");

        assertNotNull(result);
        assertTrue(result.contains("Test Member"));
        mockServer.verify();
    }

    @Test
    void getConsortium_shouldReturnData() {
        expectTokenRequest();
        expectQueryRequest("SF_CONS_1", "{\"records\":[{\"Name\":\"Consortium\"}]}");

        String result = salesforceClient.getConsortium("SF_CONS_1");

        assertNotNull(result);
        assertTrue(result.contains("Consortium"));
        mockServer.verify();
    }

    @Test
    void getMemberContacts_shouldReturnData() {
        expectTokenRequest();
        expectQueryRequest("SF_ORG_1", "{\"records\":[{\"Contact__c\":\"Contact_1\"}]}");

        String result = salesforceClient.getMemberContacts("SF_ORG_1");

        assertNotNull(result);
        assertTrue(result.contains("Contact_1"));
        mockServer.verify();
    }

    @Test
    void getMemberContactData_shouldReturnData() {
        expectTokenRequest();
        expectQueryRequest("CONTACT_123", "{\"records\":[{\"Email\":\"test@test.com\"}]}");

        String result = salesforceClient.getMemberContactData("CONTACT_123");

        assertNotNull(result);
        assertTrue(result.contains("test@test.com"));
        mockServer.verify();
    }

    @Test
    void getMemberOrgIds_shouldReturnData() {
        expectTokenRequest();
        expectQueryRequest("SF_123", "{\"records\":[{\"Identifier_Type__c\":\"ROR\"}]}");

        String result = salesforceClient.getMemberOrgIds("SF_123");

        assertNotNull(result);
        assertTrue(result.contains("ROR"));
        mockServer.verify();
    }

    @Test
    void getMembers_shouldReturnData() {
        expectTokenRequest();
        expectQueryRequest("Active_Member__c", "{\"records\":[{\"Name\":\"Mock Member\"}]}");

        String result = salesforceClient.getMembers();

        assertNotNull(result);
        assertTrue(result.contains("Mock Member"));
        mockServer.verify();
    }

    @Test
    void fetchDataFromUrl_shouldReturnData() {
        expectTokenRequest();

        String nextRecordsUrl = "/services/data/v60.0/query/01gD0000002huKiIAI-500";
        String expectedFullUrl = LOGIN_URL + nextRecordsUrl;

        mockServer.expect(requestTo(expectedFullUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withSuccess("{\"records\":[{\"Name\":\"Next Page Member\"}]}", MediaType.APPLICATION_JSON));

        String result = salesforceClient.fetchDataFromUrl(nextRecordsUrl);

        assertNotNull(result);
        assertTrue(result.contains("Next Page Member"));
        mockServer.verify();
    }

    @Test
    void getMetadata_shouldParseAndReturnMap() {
        expectTokenRequest();

        mockServer.expect(requestTo(BASE_URL + "/sobjects/Account/describe"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withSuccess("{\"fields\": [{\"name\": \"Id\"}]}", MediaType.APPLICATION_JSON));

        Map<String, Object> metadata = salesforceClient.getMetadata();

        assertNotNull(metadata);
        assertTrue(metadata.containsKey("fields"));
        mockServer.verify();
    }

    // ==========================================
    // SUCCESSFUL POST/PATCH TESTS
    // ==========================================

    @Test
    void updatePublicMemberDetails_shouldPatchCorrectly() {
        expectTokenRequest();

        String salesforceId = "SF_999";
        mockServer.expect(requestTo(BASE_URL + "/sobjects/Account/" + salesforceId + "?_HttpMethod=PATCH"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withNoContent());

        MemberUpdateData updateData = new MemberUpdateData();
        updateData.setSalesforceId(salesforceId);
        updateData.setOrgName("New Org Name");

        assertDoesNotThrow(() -> salesforceClient.updatePublicMemberDetails(updateData));
        mockServer.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void request_shouldRefreshTokenOnExceptionAndRetry() {
        AtomicReference<String> tokenRef = (AtomicReference<String>) ReflectionTestUtils.getField(salesforceClient, "accessToken");
        assertNotNull(tokenRef);
        tokenRef.set("expired-token");

        // 1. Initial request fails with 401
        mockServer.expect(requestTo(org.hamcrest.CoreMatchers.startsWith(BASE_URL + "/query")))
                .andExpect(header("Authorization", "Bearer expired-token"))
                .andRespond(withUnauthorizedRequest());

        // 2. Automatically requests new token
        expectTokenRequest();

        // 3. Retries original request with new token
        mockServer.expect(requestTo(org.hamcrest.CoreMatchers.startsWith(BASE_URL + "/query")))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withSuccess("{\"records\":[]}", MediaType.APPLICATION_JSON));

        String result = salesforceClient.getMemberDetails("SF_123");

        assertNotNull(result);
        mockServer.verify();
    }

    @Test
    void get_non2xxResponse_shouldReturnNull() {
        expectTokenRequest();

        // Simulate a 500 Internal Server Error
        mockServer.expect(requestTo(org.hamcrest.CoreMatchers.startsWith(BASE_URL + "/query")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body("Internal Server Error"));

        // The client catches non-auth RestClientResponseExceptions and returns null
        String result = salesforceClient.getMemberDetails("SF_123");

        assertNull(result);
        mockServer.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_403ForbiddenResponse_shouldThrowException() {
        // Setup initial token to bypass token creation block
        AtomicReference<String> tokenRef = (AtomicReference<String>) ReflectionTestUtils.getField(salesforceClient, "accessToken");
        assertNotNull(tokenRef);
        tokenRef.set(MOCK_TOKEN);

        // Fail once, triggering a retry
        mockServer.expect(requestTo(org.hamcrest.CoreMatchers.startsWith(BASE_URL + "/query")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        // Fetch new token
        expectTokenRequest();

        // Fail again on the retry
        mockServer.expect(requestTo(org.hamcrest.CoreMatchers.startsWith(BASE_URL + "/query")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        // The client explicitly throws 401 and 403 errors
        assertThrows(RestClientResponseException.class, () -> salesforceClient.getMemberDetails("SF_123"));
        mockServer.verify();
    }

    @Test
    void post_non2xxResponse_shouldBeCaughtAndLogged() {
        expectTokenRequest();

        String salesforceId = "SF_999";
        // Simulate a 500 Error during POST
        mockServer.expect(requestTo(BASE_URL + "/sobjects/Account/" + salesforceId + "?_HttpMethod=PATCH"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        MemberUpdateData updateData = new MemberUpdateData();
        updateData.setSalesforceId(salesforceId);

        // The exception should be caught and logged internally, not bubbled up
        assertDoesNotThrow(() -> salesforceClient.updatePublicMemberDetails(updateData));
        mockServer.verify();
    }

    @Test
    void getMetadata_badJsonResponse_shouldThrowRuntimeException() {
        // First attempt
        expectTokenRequest();
        mockServer.expect(requestTo(BASE_URL + "/sobjects/Account/describe"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"fields\": [{\"name\": \"Id\"}", MediaType.APPLICATION_JSON));

        // The blanket catch(Exception) in request() triggers a token refresh and a second attempt
        expectTokenRequest();
        mockServer.expect(requestTo(BASE_URL + "/sobjects/Account/describe"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"fields\": [{\"name\": \"Id\"}", MediaType.APPLICATION_JSON));

        // The exception will successfully bubble up after the second failed attempt
        RuntimeException exception = assertThrows(RuntimeException.class, () -> salesforceClient.getMetadata());
        assertEquals("Error getting salesforce metadata", exception.getMessage());
        mockServer.verify();
    }

    @Test
    void createAccessToken_failureShouldThrowRuntimeException() {
        // Simulate a failure reaching the login/token URL
        mockServer.expect(MockRestRequestMatchers.requestTo(LOGIN_URL + "/services/oauth2/token"))
                .andRespond(withServerError());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> salesforceClient.getMemberDetails("SF_123"));
        assertTrue(exception.getMessage().contains("java.lang.RuntimeException"));
        mockServer.verify();
    }

    @Test
    void fetchDataFromUrl_non2xxResponse_shouldReturnNull() {
        expectTokenRequest();

        String nextRecordsUrl = "/services/data/v60.0/query/01gD0000002huKiIAI-500";
        String expectedFullUrl = LOGIN_URL + nextRecordsUrl;

        // Simulate a 500 Internal Server Error
        mockServer.expect(requestTo(expectedFullUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body("Internal Server Error"));

        // The client catches non-auth RestClientResponseExceptions and returns null
        String result = salesforceClient.fetchDataFromUrl(nextRecordsUrl);

        assertNull(result);
        mockServer.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchDataFromUrl_401Unauthorized_shouldRetryAndReturnData() {
        // Setup initial expired token
        AtomicReference<String> tokenRef = (AtomicReference<String>) ReflectionTestUtils.getField(salesforceClient, "accessToken");
        assertNotNull(tokenRef);
        tokenRef.set("expired-token");

        String nextRecordsUrl = "/services/data/v60.0/query/01gD0000002huKiIAI-500";
        String expectedFullUrl = LOGIN_URL + nextRecordsUrl;

        // 1. Initial request fails with 401
        mockServer.expect(requestTo(expectedFullUrl))
                .andExpect(header("Authorization", "Bearer expired-token"))
                .andRespond(withUnauthorizedRequest());

        // 2. Automatically requests new token
        expectTokenRequest();

        // 3. Retries original request with new token
        mockServer.expect(requestTo(expectedFullUrl))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withSuccess("{\"records\":[{\"Name\":\"Next Page Member\"}]}", MediaType.APPLICATION_JSON));

        String result = salesforceClient.fetchDataFromUrl(nextRecordsUrl);

        assertNotNull(result);
        assertTrue(result.contains("Next Page Member"));
        mockServer.verify();
    }

    private void expectTokenRequest() {
        String tokenResponse = String.format("{\"access_token\":\"%s\"}", MOCK_TOKEN);
        mockServer.expect(MockRestRequestMatchers.requestTo(LOGIN_URL + "/services/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(tokenResponse, MediaType.APPLICATION_JSON));
    }

    private void expectQueryRequest(String id, String responseBody) {
        // Using startsWith allows us to bypass checking the complex URL-encoded SOQL string
        mockServer.expect(requestTo(org.hamcrest.CoreMatchers.startsWith(BASE_URL + "/query?q=")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + MOCK_TOKEN))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }
}