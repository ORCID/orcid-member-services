package org.orcid.mp.member.client;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.orcid.mp.member.salesforce.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class SalesforceClient {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceClient.class);

    private String accessToken;

    @Value("${application.salesforce.orcidApiClientId")
    private String orcidApiClientId;

    @Value("${application.salesforce.orcidApiClientSecret}")
    private String orcidApiClientSecret;

    @Value("${application.salesforce.clientEndpoint}")
    private String salesforceClientEndpoint;

    @Value("${application.salesforce.tokenEndpoint}")
    private String salesforceTokenEndpoint;

    @Autowired
    @Qualifier("salesforceRestClient")
    private RestClient restClient;

    public MemberDetails getMemberDetails(String salesforceId) throws IOException {
        return request(() -> getSFMemberDetails(salesforceId));
    }

    public Boolean updatePublicMemberDetails(MemberUpdateData memberUpdateData) throws IOException {
        return request(() -> updateSFPublicMemberDetails(memberUpdateData));
    }

    public MemberContacts getMemberContacts(String salesforceId) throws IOException {
        return request(() -> getSFMemberContacts(salesforceId));
    }

    public MemberOrgIds getMemberOrgIds(String salesforceId) throws IOException {
        return request(() -> getSFMemberOrgIds(salesforceId));
    }

    public ConsortiumLeadDetails getConsortiumLeadDetails(String salesforceId) throws IOException {
        return request(() -> getSFConsortiumLeadDetails(salesforceId));
    }

    public List<Country> getSalesforceCountries() {
        return request(() -> getSFCountryData());
    }

    private List<Country> getSFCountryData() {
        return get("/countries", new ParameterizedTypeReference<List<Country>>() {
        });
    }

    private Boolean updateSFPublicMemberDetails(MemberUpdateData memberUpdateData) {
        String success = put("/member/" + memberUpdateData.getSalesforceId() + "/member-data", memberUpdateData, new ParameterizedTypeReference<String>() {
        });
        return success != null;
    }

    private MemberDetails getSFMemberDetails(String salesforceId) {
        return get("/member/" + salesforceId + "/details", new ParameterizedTypeReference<MemberDetails>() {
        });
    }

    private MemberContacts getSFMemberContacts(String salesforceId) {
        return get("/member/" + salesforceId + "/contacts", new ParameterizedTypeReference<MemberContacts>() {
        });
    }

    private MemberOrgIds getSFMemberOrgIds(String salesforceId) {
        return get("/member/" + salesforceId + "/org-ids", new ParameterizedTypeReference<MemberOrgIds>() {
        });
    }

    private ConsortiumLeadDetails getSFConsortiumLeadDetails(String salesforceId) {
        return get("/member/" + salesforceId + "/details", new ParameterizedTypeReference<ConsortiumLeadDetails>() {
        });
    }

    private <T> T get(String path, ParameterizedTypeReference<T> typeReference) {
        ResponseEntity<T> response = restClient.get().uri(salesforceClientEndpoint + path).headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve().toEntity(typeReference);
        return processResponse(response, path);
    }

    private <T> T put(String path, MemberUpdateData updateData, ParameterizedTypeReference<T> typeReference) {
        ResponseEntity<T> response = restClient.put().uri(salesforceClientEndpoint + path).body(updateData).headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve().toEntity(typeReference);
        return processResponse(response, path);
    }

    private <T> T processResponse(ResponseEntity<T> response, String path) {
        HttpStatusCode statusCode = response.getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            LOG.warn("Received non-200 response from salesforce client path {}", path);
            LOG.info("Response code is {}", statusCode.toString());
            LOG.info("Response body is {}", response.getBody() != null ? response.getBody().toString() : "<empty>");
            return null;
        }
        return response.getBody();
    }

    private <T> T request(Supplier<T> function) {
        initAccessToken();
        try {
            return function.get();
        } catch (Exception e) {
            LOG.info("Refreshing access token");
            createAccessToken();
            return function.get();
        }
    }

    private void initAccessToken() {
        if (accessToken == null) {
            createAccessToken();
        }
    }

    private void createAccessToken() {
        try {
            accessToken = getAccessToken();
        } catch (Exception e) {
            LOG.error("Failed to create internal access token", e);
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken() {
        LOG.info("Acquiring access token...");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", orcidApiClientId);
        formData.add("client_secret", orcidApiClientSecret);
        formData.add("scope", "/member-list/full-access /member-list/read");
        formData.add("grant_type", "client_credentials");

        try {
            String responseString = restClient.post().uri(salesforceTokenEndpoint).contentType(MediaType.APPLICATION_FORM_URLENCODED) // Set Content-Type header
                    .body(formData)
                    .retrieve().body(String.class);

            LOG.info("Access token acquired successfully");

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseString);
            return responseJson.get("access_token").textValue();
        } catch (Exception ex) {
            LOG.error("Failed to acquire access token: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error while acquiring access token", ex);
        }
    }
}