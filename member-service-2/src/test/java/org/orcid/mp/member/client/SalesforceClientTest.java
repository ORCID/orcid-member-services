package org.orcid.mp.member.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.salesforce.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesforceClientTest {

    @InjectMocks
    private SalesforceClient salesforceClient;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String CLIENT_ENDPOINT = "http://salesforce.com/api";
    private static final String TOKEN_ENDPOINT = "http://salesforce.com/token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(salesforceClient, "orcidApiClientId", CLIENT_ID);
        ReflectionTestUtils.setField(salesforceClient, "orcidApiClientSecret", CLIENT_SECRET);
        ReflectionTestUtils.setField(salesforceClient, "salesforceClientEndpoint", CLIENT_ENDPOINT);
        ReflectionTestUtils.setField(salesforceClient, "salesforceTokenEndpoint", TOKEN_ENDPOINT);
    }

    @Test
    void testGetMemberDetails_Success() throws IOException {
        mockAccessTokenCall("fake-access-token");

        MemberDetails mockDetails = new MemberDetails();

        mockGetRequest("/member/123/details", mockDetails);

        MemberDetails result = salesforceClient.getMemberDetails("123");

        assertNotNull(result);
        assertEquals(mockDetails, result);

        verify(restClient).post();
    }

    @Test
    void testUpdatePublicMemberDetails_Success() throws IOException {
        mockAccessTokenCall("fake-access-token");

        MemberUpdateData updateData = new MemberUpdateData();
        updateData.setSalesforceId("123");

        mockPutRequest("/member/123/member-data", updateData, "Success");

        Boolean result = salesforceClient.updatePublicMemberDetails(updateData);

        assertTrue(result);
    }

    @Test
    void testGetMemberDetails_RetryLogic() throws IOException {
        mockAccessTokenCall("initial-token");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(contains("/member/123/details"))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Token expired")) // First call fails
                .thenReturn(ResponseEntity.ok(new MemberDetails())); // Second call succeeds

        MemberDetails result = salesforceClient.getMemberDetails("123");
        assertNotNull(result);
        verify(restClient, times(2)).post();
    }

    @Test
    void testGetSalesforceCountries() {
        mockAccessTokenCall("token");
        List<Country> countries = List.of(new Country(), new Country());

        mockGetRequest("/countries", countries);

        List<Country> result = salesforceClient.getSalesforceCountries();
        assertEquals(2, result.size());
    }

    @Test
    void testGetConsortiumLeadDetails_Success() throws IOException {
        mockAccessTokenCall("token");
        ConsortiumLeadDetails mockConsortiumDetails = new ConsortiumLeadDetails();
        mockGetRequest("/member/123/details", mockConsortiumDetails);

        ConsortiumLeadDetails result = salesforceClient.getConsortiumLeadDetails("123");

        assertNotNull(result);
        assertEquals(mockConsortiumDetails, result);
    }

    @Test
    void testGetMemberContacts_Success() throws IOException {
        mockAccessTokenCall("token");

        MemberContacts mockContacts = new MemberContacts();
        mockGetRequest("/member/123/contacts", mockContacts);

        MemberContacts result = salesforceClient.getMemberContacts("123");
        assertNotNull(result);
        assertEquals(mockContacts, result);
    }

    @Test
    void testProcessResponse_Non2xx() {
        mockAccessTokenCall("token");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ResponseEntity responseEntity = new ResponseEntity<>("Error Body", HttpStatus.INTERNAL_SERVER_ERROR);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        MemberOrgIds result = null;
        try {
            result = salesforceClient.getMemberOrgIds("123");
        } catch (IOException e) {
            fail("Should not throw IO exception");
        }

        assertNull(result);
    }

    private void mockAccessTokenCall(String tokenValue) {
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(TOKEN_ENDPOINT)).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        String jsonResponse = "{\"access_token\": \"" + tokenValue + "\"}";
        lenient().when(responseSpec.body(String.class)).thenReturn(jsonResponse);
    }

    private <T> void mockGetRequest(String pathSuffix, T responseBody) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(CLIENT_ENDPOINT + pathSuffix)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ResponseEntity<T> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
    }

    private <T> void mockPutRequest(String pathSuffix, Object requestBody, T responseBody) {
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(CLIENT_ENDPOINT + pathSuffix)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(requestBody)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        ResponseEntity<T> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
    }
}