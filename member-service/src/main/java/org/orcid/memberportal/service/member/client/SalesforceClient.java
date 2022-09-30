package org.orcid.memberportal.service.member.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.memberportal.service.member.client.model.ConsortiumLeadDetails;
import org.orcid.memberportal.service.member.client.model.ConsortiumMember;
import org.orcid.memberportal.service.member.client.model.MemberContacts;
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.config.ApplicationProperties;
import org.orcid.memberportal.service.member.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

@Component
public class SalesforceClient {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceClient.class);

    private CloseableHttpClient httpClient;
    
    private String accessToken;

    @Autowired
    private ApplicationProperties applicationProperties;
    
    public SalesforceClient() {
        this.httpClient = HttpClients.createDefault();
    }
    
    public MemberDetails getMemberDetails(String salesforceId) throws IOException {
    	return request(() -> {
            return getSFMemberDetails(salesforceId);
        });
    }
    
    public MemberContacts getMemberContacts(String salesforceId) throws IOException {
    	return request(() -> {
            return getSFMemberContacts(salesforceId);
        });
    }
    
    public ConsortiumLeadDetails getConsortiumLeadDetails(String salesforceId) throws IOException {
    	return request(() -> {
            return getSFConsortiumLeadDetails(salesforceId);
        });
    }
    
    private MemberDetails getSFMemberDetails(String salesforceId) {
        try (CloseableHttpResponse response = getMemberDetailsResponse(salesforceId)) {
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                logError(salesforceId, response);
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                JsonNode root = objectMapper.readTree(response.getEntity().getContent());
                return objectMapper.treeToValue(root.at("/member"), MemberDetails.class);
            }
        } catch (IOException e) {
        	LOG.error("Error getting member details from salesforce", e);
        	throw new RuntimeException(e);
        }
        return null;
    }
    
    private MemberContacts getSFMemberContacts(String salesforceId) {
        try (CloseableHttpResponse response = getMemberContactsResponse(salesforceId)) {
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                logError(salesforceId, response);
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                return objectMapper.readValue(response.getEntity().getContent(), MemberContacts.class);
            }
        } catch (IOException e) {
        	LOG.error("Error getting member contacts from salesforce", e);
        	throw new RuntimeException(e);
        }
        return null;
    }
    
    private ConsortiumLeadDetails getSFConsortiumLeadDetails(String salesforceId) {
        try (CloseableHttpResponse response = getMemberDetailsResponse(salesforceId)) {
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                logError(salesforceId, response);
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                JsonNode root = objectMapper.readTree(response.getEntity().getContent());
                ConsortiumLeadDetails consortiumLeadDetails = objectMapper.treeToValue(root.at("/member"), ConsortiumLeadDetails.class);
                
                ObjectReader reader = objectMapper.readerFor(new TypeReference<List<ConsortiumMember>>(){});
                List<ConsortiumMember> consortiumMembers = reader.readValue(root.at("/consortiumOpportunities"));
                consortiumLeadDetails.setConsortiumMembers(consortiumMembers);
                return consortiumLeadDetails;
            }
        } catch (IOException e) {
        	LOG.error("Error getting consortium member details from salesforce", e);
        	throw new RuntimeException(e);
        }
        return null;
    }

    private void logError(String salesforceId, CloseableHttpResponse response) throws IOException {
        LOG.warn("Received non-200 response trying to find member details for {}", salesforceId);
        String responseString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        LOG.warn("Response received:");
        LOG.warn(responseString);
    }
    
    private CloseableHttpResponse getMemberContactsResponse(String salesforceId) throws IOException {
        return sfGet("member/" + salesforceId + "/contacts");
    }
    
    private CloseableHttpResponse getMemberDetailsResponse(String salesforceId) throws IOException {
        return sfGet("member/" + salesforceId + "/details");
    }
    
    private CloseableHttpResponse sfGet(String path) throws IOException {
    	String endpoint = applicationProperties.getSalesforceClientEndpoint();
    	HttpGet httpGet = new HttpGet(endpoint + path);
    	httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return httpClient.execute(httpGet);
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
    
    private String getAccessToken() throws JSONException, ClientProtocolException, IOException {
    	LOG.info("Acquiring access token...");
        HttpPost httpPost = new HttpPost(applicationProperties.getOrcidApiTokenEndpoint());

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("client_id", applicationProperties.getOrcidApiClientId()));
        params.add(new BasicNameValuePair("client_secret", applicationProperties.getOrcidApiClientSecret()));
        params.add(new BasicNameValuePair("scope", "/member-list/full-access /member-list/read"));
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        HttpResponse response = httpClient.execute(httpPost);
        Integer statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != Status.OK.getStatusCode()) {
            String responseString = EntityUtils.toString(response.getEntity());
            LOG.error("Failed to obtain salesforce client access token: {}", responseString);
            throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
        }

        LOG.info("Access token acquired");
        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject json = new JSONObject(responseString);

        return json.get("access_token").toString();
    }
    
}
