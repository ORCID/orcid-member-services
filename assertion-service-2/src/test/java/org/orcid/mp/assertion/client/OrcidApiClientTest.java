package org.orcid.mp.assertion.client;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.mp.assertion.domain.AffiliationSection;
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.error.DeactivatedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrcidApiClientTest {

    private OrcidApiClient orcidApiClient;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<Affiliation> affiliationCaptor;

    private final String API_URL = "https://api.orcid.org/";
    private final String INTERNAL_API_URL = "https://internal.orcid.org/";
    private final String TOKEN_EXCHANGE_URL = "https://orcid.org/oauth/token";

    @BeforeEach
    void setUp() {
        orcidApiClient = new OrcidApiClient();

        ReflectionTestUtils.setField(orcidApiClient, "restClient", restClient);
        ReflectionTestUtils.setField(orcidApiClient, "apiUrl", API_URL);
        ReflectionTestUtils.setField(orcidApiClient, "internalApiUrl", INTERNAL_API_URL);
        ReflectionTestUtils.setField(orcidApiClient, "tokenExchangeUrl", TOKEN_EXCHANGE_URL);
        ReflectionTestUtils.setField(orcidApiClient, "clientId", "client-id");
        ReflectionTestUtils.setField(orcidApiClient, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(orcidApiClient, "grantType", "authorization_code");
        ReflectionTestUtils.setField(orcidApiClient, "subjectTokenType", "urn:ietf:params:oauth:token-type:id_token");
        ReflectionTestUtils.setField(orcidApiClient, "requestedTokenType", "urn:ietf:params:oauth:token-type:access_token");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(restClient.delete()).thenReturn(requestHeadersUriSpec);

        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testExchangeTokenSuccess() throws IOException, JSONException, DeactivatedException {
        String mockResponse = "{\"access_token\": \"new-access-token\"}";
        when(responseSpec.toEntity(String.class)).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        String token = orcidApiClient.exchangeToken("id-token", "orcid-id");

        assertThat(token).isEqualTo("new-access-token");
        verify(restClient).post();
    }

    @Test
    void testExchangeTokenDeactivated() {
        String errorResponse = "{\"error\": \"invalid_scope\"}";

        when(responseSpec.toEntity(String.class))
                .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED))
                .thenReturn(new ResponseEntity<>("{\"access_token\":\"internal\"}", HttpStatus.OK))
                .thenReturn(new ResponseEntity<>("Deactivated", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> orcidApiClient.exchangeToken("id-token", "orcid-id"))
                .isInstanceOf(DeactivatedException.class);
    }

    @Test
    void testPostAffiliationSuccess() throws Exception {
        String orcid = "0000-0000";
        String accessToken = "access-token";
        Assertion assertion = createMockAssertion();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "http://api.orcid.org/v3.0/0000-0000/employment/12345");

        when(responseSpec.toEntity(String.class))
                .thenReturn(new ResponseEntity<>("", headers, HttpStatus.CREATED));

        String putCode = orcidApiClient.postAffiliation(orcid, accessToken, assertion);

        assertThat(putCode).isEqualTo("12345");

        verify(requestBodySpec).body(affiliationCaptor.capture());
        assertThat(affiliationCaptor.getValue().getRoleTitle()).isEqualTo("Researcher");
    }

    @Test
    void testPutAffiliationSuccess() throws Exception {
        String orcid = "0000-0000";
        String accessToken = "access-token";
        Assertion assertion = createMockAssertion();
        assertion.setPutCode("12345");

        when(responseSpec.toEntity(String.class))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));

        orcidApiClient.putAffiliation(orcid, accessToken, assertion);

        verify(restClient).put();
    }

    @Test
    void testDeleteAffiliationSuccess() throws Exception {
        String orcid = "0000-0000";
        String accessToken = "access-token";
        Assertion assertion = createMockAssertion();
        assertion.setPutCode("12345");

        when(responseSpec.toEntity(String.class))
                .thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

        orcidApiClient.deleteAffiliation(orcid, accessToken, assertion);

        verify(restClient).delete();
    }

    @Test
    void testGetOrcidIdForEmail_Success() throws IOException {
        String internalTokenResponse = "{\"access_token\": \"internal-token\"}";
        String registryResponse = "{\"orcid\": \"0000-0000-0000-0001\"}";

        when(responseSpec.toEntity(String.class))
                .thenReturn(new ResponseEntity<>(internalTokenResponse, HttpStatus.OK))
                .thenReturn(new ResponseEntity<>(registryResponse, HttpStatus.OK));

        String result = orcidApiClient.getOrcidIdForEmail("test@test.com");

        assertThat(result).isEqualTo("0000-0000-0000-0001");
    }

    private Assertion createMockAssertion() {
        Assertion assertion = new Assertion();
        assertion.setRoleTitle("Researcher");
        assertion.setAffiliationSection(AffiliationSection.EMPLOYMENT);
        assertion.setOrgCountry("US");
        return assertion;
    }
}