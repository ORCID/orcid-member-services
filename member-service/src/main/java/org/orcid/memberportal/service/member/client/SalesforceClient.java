package org.orcid.memberportal.service.member.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.orcid.memberportal.service.member.client.model.ConsortiumLeadDetails;
import org.orcid.memberportal.service.member.client.model.ConsortiumMember;
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.config.ApplicationProperties;
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

    @Autowired
    private ApplicationProperties applicationProperties;
    
    public SalesforceClient() {
        this.httpClient = HttpClients.createDefault();
    }

    public MemberDetails getMemberDetails(String salesforceId) throws IOException {
        try (CloseableHttpResponse response = getMemberDetailsResponse(salesforceId)) {
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                logError(salesforceId, response);
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                JsonNode root = objectMapper.readTree(response.getEntity().getContent());
                return objectMapper.treeToValue(root.at("/member"), MemberDetails.class);
            }
        }
        return null;
    }
    
    public ConsortiumLeadDetails getConsortiumLeadDetails(String salesforceId) throws IOException {
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
        }
        return null;
    }

    private void logError(String salesforceId, CloseableHttpResponse response) throws IOException {
        LOG.warn("Received non-200 response trying to find member details for {}", salesforceId);
        String responseString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        LOG.warn("Response received:");
        LOG.warn(responseString);
    }
    
    private CloseableHttpResponse getMemberDetailsResponse(String salesforceId) throws IOException {
        String endpoint = applicationProperties.getSalesforceClientEndpoint();
        HttpGet httpGet = new HttpGet(endpoint + "member/" + salesforceId + "/details");
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + applicationProperties.getSalesforceClientToken());
        return httpClient.execute(httpGet);
    }
    
}
