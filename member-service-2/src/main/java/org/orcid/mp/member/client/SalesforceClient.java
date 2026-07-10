package org.orcid.mp.member.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.orcid.mp.member.salesforce.MemberUpdateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public class SalesforceClient {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceClient.class);

    private final AtomicReference<String> accessToken = new AtomicReference<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${application.salesforce.apiBaseUrl}")
    private String salesforceAPIBaseUrl;

    @Value("${application.salesforce.username}")
    private String username;

    @Value("${application.salesforce.password}")
    private String password;

    @Value("${application.salesforce.clientId}")
    private String clientId;

    @Value("${application.salesforce.clientSecret}")
    private String clientSecret;

    @Value("${application.salesforce.loginUrl}")
    private String loginUrl;

    @Autowired
    @Qualifier("salesforceRestClient")
    private RestClient restClient;

    public String getMemberDetails(String salesforceId) {
        LOG.debug("Fetching member details from salesforce...");
        return request(() -> getSFMemberDetails(salesforceId));
    }

    public String getConsortium(String salesforceId) {
        LOG.debug("Fetching consortium from salesforce...");
        return request(() -> getSFConsortium(salesforceId));
    }

    public String getMemberContacts(String salesforceId) {
        return request(() -> getSFMemberContacts(salesforceId));
    }

    public String getMemberContactData(String contactId) {
        return request(() -> getSFMemberContactData(contactId));
    }

    public void updatePublicMemberDetails(MemberUpdateData memberUpdateData) {
        request(() -> updateSFPublicMemberDetails(memberUpdateData));
    }

    public String getMembers() {
        return request(() -> getSFMembers());
    }

    public String fetchDataFromUrl(String nextRecordsUrl) {
        LOG.debug("Fetching paginated data from salesforce url: {}", nextRecordsUrl);
        return request(() -> getSFDataFromUrl(nextRecordsUrl));
    }

    private Boolean updateSFPublicMemberDetails(MemberUpdateData memberUpdateData) {
        String salesforceId = memberUpdateData.getSalesforceId();
        Map<String, Object> data = getDataMapForUpdate(memberUpdateData);
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            post("/sobjects/Account/" + salesforceId + "?_HttpMethod=PATCH", jsonData);
            return true;
        } catch (JsonProcessingException e) {
            LOG.warn("Error writing member JSON to update", e);
            throw new RuntimeException("Error writing member JSON to update", e);
        }
    }

    public String getMemberOrgIds(String salesforceId) {
        return request(() -> getSFMemberOrgIds(salesforceId));
    }

    public Map<String, Object> getMetadata() {
        return request(() -> getSFMetadata());
    }

    private String getSFDataFromUrl(String nextRecordsUrl) {
        String fullUrl = loginUrl + nextRecordsUrl;

        LOG.debug("Sending salesforce GET request for next page: {}", fullUrl);
        try {
            ResponseEntity<String> response = restClient.get().uri(fullUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken.get()))
                    .retrieve()
                    .toEntity(String.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw ex;
            }

            LOG.warn("Received non-2xx response from salesforce paginated query to {}", fullUrl, ex);
            LOG.info("Response code is {}", ex.getStatusCode());
            LOG.info("Response body is {}", ex.getResponseBodyAsString());
            return null;
        }
    }

    private Map<String, Object> getDataMapForUpdate(MemberUpdateData memberData) {
        Map<String, Object> data = new HashMap<>();
        data.put("Name", memberData.getOrgName());
        data.put("Public_Display_Name__c", memberData.getPublicName());
        data.put("Public_Display_Description__c", memberData.getDescription());
        data.put("Public_Display_Email__c", memberData.getEmail());
        data.put("Website", memberData.getWebsite());

        if (memberData.getTrademarkLicense() != null) {
            data.put("Trademark_License__c", memberData.getTrademarkLicense());
        }

        if (memberData.getBillingAddress() != null) {
            data.put("BillingCity", memberData.getBillingAddress().getCity());
            data.put("BillingCountry", memberData.getBillingAddress().getCountry());
            data.put("BillingCountryCode", memberData.getBillingAddress().getCountryCode());
            data.put("BillingPostalCode", memberData.getBillingAddress().getPostalCode());
            data.put("BillingState", memberData.getBillingAddress().getState());
            data.put("BillingStateCode", memberData.getBillingAddress().getStateCode());
            data.put("BillingStreet", memberData.getBillingAddress().getStreet());
        }
        return data;
    }

    private Map<String, Object> getSFMetadata() {
        LOG.debug("Sending salesforce GET request for metadata");
        try {
            String metadataJson = restClient.get().uri(salesforceAPIBaseUrl + "/sobjects/Account/describe")
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken.get()))
                    .retrieve()
                    .body(String.class);

            if (metadataJson != null) {
                return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw ex;
            }
            LOG.warn("Received non-2xx response from salesforce metadata request", ex);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse metadata JSON", e);
        }

        throw new RuntimeException("Error getting salesforce metadata");
    }

    private String getSFMemberDetails(String salesforceId) {
        String query = String.format(
                "SELECT Account.Id, Account.Consortium_Lead__c, Account.OwnerId, Account.Name, Account.Public_Display_Name__c, Account.Website, Account.BillingCountry, Account.Research_Community__c, Account.Consortia_Member__c, RecordTypeId, "
                        + "Account.Public_Display_Description__c, Account.Logo_Description__c, Account.Public_Display_Email__c, Account.Last_membership_start_date__c, Account.Last_membership_end_date__c, Account.Trademark_License__c, Account.BillingAddress, Account.Active_Member__c FROM Account "
                        + "WHERE Account.Id = '%s'",
                salesforceId);
        return query(query);
    }

    private String getSFConsortium(String salesforceId) {
        String query = String.format(
                "SELECT (SELECT Id, AccountId, Account.Name, Account.Public_Display_Name__c, StageName, NextStep, Consortium_member_removal_requested__c, Consortia_Lead__c FROM ConsortiaOpportunities__r WHERE StageName In ('Negotiation/Review', 'Invoice Paid', 'Agreement Signed', 'Invoice Sent', 'Partial Payment', 'In Collections') AND Membership_Start_Date__c<=TODAY AND Membership_End_Date__c>TODAY ORDER BY Account.Public_Display_Name__c) from Account WHERE Id='%s'",
                salesforceId);
        return query(query);
    }

    private String getSFMemberContacts(String salesforceId) {
        String query = String.format(
                "SELECT Membership_Contact_Role__c.Contact__c, Membership_Contact_Role__c.Member_Org_Role__c, Membership_Contact_Role__c.Contact_Curr_Email__c, Membership_Contact_Role__c.Voting_Contact__c, Membership_Contact_Role__c.Organization__c FROM Membership_Contact_Role__c "
                        + "WHERE Current__c=TRUE AND Membership_Contact_Role__c.Organization__c = '%s'",
                salesforceId);
        return query(query);
    }

    private String getSFMemberContactData(String contactId) {
        String query = String.format("SELECT Name, Email, Title, Phone FROM Contact WHERE Id = '%s'", contactId);
        return query(query);
    }

    private String getSFMemberOrgIds(String salesforceId) {
        String query = String.format(
                "SELECT Identifier_Type__c, Name FROM Organization_Identifier__c WHERE (Identifier_Type__c = 'Ringgold ID' OR Identifier_Type__c = 'FundRef ID' OR Identifier_Type__c = 'GRID' OR Identifier_Type__c = 'ROR') AND Organization__c = '%s'",
                salesforceId);
        return query(query);
    }

    private String getSFMembers() {
        String query = "SELECT Account.Id, Account.Consortium_Lead__c, Account.OwnerId, Account.Name, Account.Public_Display_Name__c, Account.Website, Account.BillingCountry, Account.Research_Community__c, Account.Consortia_Member__c, RecordTypeId, "
                + "Account.Public_Display_Description__c, Account.Logo_Description__c, Account.Public_Display_Email__c, Account.Last_membership_start_date__c, Account.Last_membership_end_date__c, Account.Active_Member__c FROM Account "
                + "WHERE Active_Member__c=TRUE";
        return query(query);
    }

    private String query(String query) {
        LOG.debug("Sending salesforce GET request for query {}", query);
        try {
            ResponseEntity<String> response = restClient.get().uri(salesforceAPIBaseUrl + "/query?q={query}", query)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken.get()))
                    .retrieve().toEntity(String.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw ex;
            }

            LOG.warn("Received non-2xx response from salesforce query {}", query, ex);
            LOG.info("Response code is {}", ex.getStatusCode());
            LOG.info("Response body is {}", ex.getResponseBodyAsString());
            return null;
        }
    }

    private void post(String path, String updateDataJson) {
        String url = salesforceAPIBaseUrl + path;
        try {
            restClient.post().uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updateDataJson)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken.get()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw ex;
            }

            LOG.warn("Received non-2xx response from salesforce POST to {}", url);
            LOG.info("Response code is {}", ex.getStatusCode());
            LOG.info("Response body is {}", ex.getResponseBodyAsString());
        }
    }

    private <T> T request(Supplier<T> function) {
        initAccessToken();
        try {
            return function.get();
        } catch (Exception e) {
            LOG.debug("Exception after salesforce request", e);
            LOG.info("Refreshing access token");
            synchronized (this) {
                createAccessToken();
            }
            return function.get();
        }
    }

    private void initAccessToken() {
        if (accessToken.get() == null) {
            synchronized (this) {
                if (accessToken.get() == null) {
                    createAccessToken();
                }
            }
        }
    }

    private void createAccessToken() {
        try {
            accessToken.set(getAccessToken());
        } catch (Exception e) {
            LOG.error("Failed to create access token", e);
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken() {
        LOG.info("Acquiring access token...");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", username);
        formData.add("password", password);

        try {
            String responseString = restClient.post().uri(loginUrl + "/services/oauth2/token")
                    .contentType(new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8))
                    .body(formData)
                    .retrieve().body(String.class);

            LOG.info("Access token acquired successfully");
            JsonNode responseJson = objectMapper.readTree(responseString);
            return responseJson.get("access_token").textValue();
        } catch (Exception ex) {
            LOG.error("Failed to acquire access token: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error while acquiring access token", ex);
        }
    }

}