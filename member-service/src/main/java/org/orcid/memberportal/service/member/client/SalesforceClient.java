package org.orcid.memberportal.service.member.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.memberportal.service.member.client.model.ConsortiumLeadDetails;
import org.orcid.memberportal.service.member.client.model.ConsortiumMember;
import org.orcid.memberportal.service.member.client.model.Country;
import org.orcid.memberportal.service.member.client.model.MemberContacts;
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.client.model.MemberOrgIds;
import org.orcid.memberportal.service.member.client.model.MemberUpdateData;
import org.orcid.memberportal.service.member.config.ApplicationProperties;
import org.orcid.memberportal.service.member.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

@Component
public class SalesforceClient {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceClient.class);

    private CloseableHttpClient defaultHttpClient;

    private String accessToken;

    @Autowired
    private ApplicationProperties applicationProperties;

    public MemberDetails getMemberDetails(String salesforceId) throws IOException {
        return request(() -> {
            return getSFMemberDetails(salesforceId);
        });
    }

    public Boolean updatePublicMemberDetails(MemberUpdateData memberUpdateData) throws IOException {
        return request(() -> {
            return updateSFPublicMemberDetails(memberUpdateData);
        });
    }

    public MemberContacts getMemberContacts(String salesforceId) throws IOException {
        return request(() -> {
            return getSFMemberContacts(salesforceId);
        });
    }

    public MemberOrgIds getMemberOrgIds(String salesforceId) throws IOException {
        return request(() -> {
            return getSFMemberOrgIds(salesforceId);
        });
    }

    public ConsortiumLeadDetails getConsortiumLeadDetails(String salesforceId) throws IOException {
        return request(() -> {
            return getSFConsortiumLeadDetails(salesforceId);
        });
    }

    public List<Country> getSalesforceCountries() {
        return request(() -> {
            return getSFCountryData();
        });
    }

    private List<Country> getSFCountryData() {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpGet httpGet = getGetRequest("countries");
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                    LOG.warn("Received non-200 response from salesforce client for country data");
                    logErrorBody(response);
                    EntityUtils.consume(response.getEntity());
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    return objectMapper.readValue(response.getEntity().getContent(), new TypeReference<List<Country>>() {});
                }
            } catch (IOException e) {
                LOG.error("Error getting country data from salesforce", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOG.error("HttpClient error", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private Boolean updateSFPublicMemberDetails(MemberUpdateData memberUpdateData) {
        LOG.info("Updating public details for salesforce id {}", memberUpdateData.getSalesforceId());
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpPut httpPut = getPutRequest("member/" + memberUpdateData.getSalesforceId() + "/member-data", memberUpdateData);
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                    LOG.warn("Received non-200 response from salesforce client for salesforce id {}", memberUpdateData.getSalesforceId());
                    logErrorBody(response);
                    EntityUtils.consume(response.getEntity());
                } else {
                    LOG.info("Public details for salesforce id {} updated", memberUpdateData.getSalesforceId());
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    JsonNode root = objectMapper.readTree(response.getEntity().getContent());
                    Boolean success = root.get("success").asBoolean();
                    return success;
                }
            }
        } catch (IOException e) {
            LOG.error("HttpClient error", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private MemberDetails getSFMemberDetails(String salesforceId) {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpGet httpGet = getGetRequest("member/" + salesforceId + "/details");
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                    LOG.warn("Received non-200 response from salesforce client for salesforce id {}", salesforceId);
                    logErrorBody(response);
                    EntityUtils.consume(response.getEntity());
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
        } catch (IOException e) {
            LOG.error("HttpClient error", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private MemberContacts getSFMemberContacts(String salesforceId) {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpGet httpGet = getGetRequest("member/" + salesforceId + "/contacts");
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                    LOG.warn("Received non-200 response from salesforce client for salesforce id {}", salesforceId);
                    logErrorBody(response);
                    EntityUtils.consume(response.getEntity());
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    return objectMapper.readValue(response.getEntity().getContent(), MemberContacts.class);
                }
            } catch (IOException e) {
                LOG.error("Error getting member contacts from salesforce", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOG.error("HttpClient error", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private MemberOrgIds getSFMemberOrgIds(String salesforceId) {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpGet httpGet = getGetRequest("member/" + salesforceId + "/org-ids");
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                    LOG.warn("Received non-200 response from salesforce client for salesforce id {}", salesforceId);
                    logErrorBody(response);
                    EntityUtils.consume(response.getEntity());
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    return objectMapper.readValue(response.getEntity().getContent(), MemberOrgIds.class);
                }
            } catch (IOException e) {
                LOG.error("Error getting member org ids from salesforce", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOG.error("HttpClient error", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private ConsortiumLeadDetails getSFConsortiumLeadDetails(String salesforceId) {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpGet httpGet = getGetRequest("member/" + salesforceId + "/details");
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                    LOG.warn("Received non-200 response from salesforce client for salesforce id {}", salesforceId);
                    logErrorBody(response);
                    EntityUtils.consume(response.getEntity());
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    JsonNode root = objectMapper.readTree(response.getEntity().getContent());
                    ConsortiumLeadDetails consortiumLeadDetails = objectMapper.treeToValue(root.at("/member"), ConsortiumLeadDetails.class);

                    ObjectReader reader = objectMapper.readerFor(new TypeReference<List<ConsortiumMember>>() {
                    });
                    List<ConsortiumMember> consortiumMembers = reader.readValue(root.at("/consortiumOpportunities"));
                    consortiumLeadDetails.setConsortiumMembers(consortiumMembers);
                    return consortiumLeadDetails;
                }
            } catch (IOException e) {
                LOG.error("Error getting consortium member details from salesforce", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOG.error("HttpClient error", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private HttpGet getGetRequest(String path) {
        String endpoint = applicationProperties.getSalesforceClientEndpoint();
        HttpGet httpGet = new HttpGet(endpoint + path);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return httpGet;
    }

    private HttpPut getPutRequest(String path, MemberUpdateData memberUpdateData) throws JsonProcessingException {
        String endpoint = applicationProperties.getSalesforceClientEndpoint();
        HttpPut httpPut = new HttpPut(endpoint + path);
        httpPut.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        httpPut.setEntity(getHttpEntity(memberUpdateData));
        return httpPut;
    }

    private HttpEntity getHttpEntity(MemberUpdateData memberUpdateData) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(memberUpdateData);
        return new StringEntity(json, ContentType.APPLICATION_JSON);
    }

    private CloseableHttpClient getHttpClient() {
        if (defaultHttpClient != null) {
            return defaultHttpClient;
        }

        Integer timeout = Integer.parseInt(applicationProperties.getSalesforceRequestTimeout());
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setValidateAfterInactivity(10000);
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).setSocketTimeout(timeout).build();
        return HttpClients.custom().setDefaultRequestConfig(config).setConnectionManager(connectionManager).build();
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

        try (CloseableHttpClient httpClient = getHttpClient()) {
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

    private void logErrorBody(CloseableHttpResponse response) throws IOException {
        String responseString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        LOG.warn("Response received:");
        LOG.warn(responseString);
    }

}
