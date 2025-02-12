package org.orcid.memberportal.service.assertion.client;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.orcid.jaxb.model.v3.release.error.OrcidError;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.jaxb.model.v3.release.record.Distinction;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.InvitedPosition;
import org.orcid.jaxb.model.v3.release.record.Membership;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.Service;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.adapter.AffiliationAdapter;
import org.orcid.memberportal.service.assertion.web.rest.errors.DeactivatedException;
import org.orcid.memberportal.service.assertion.web.rest.errors.DeprecatedException;
import org.orcid.memberportal.service.assertion.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;

@Component
public class OrcidAPIClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrcidAPIClient.class);

    private final Marshaller jaxbMarshaller;

    private CloseableHttpClient httpClient;

    private String internalAccessToken;

    @Autowired
    private ApplicationProperties applicationProperties;

    public OrcidAPIClient() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Affiliation.class, Distinction.class, Employment.class, Education.class, InvitedPosition.class,
            Membership.class, Qualification.class, Service.class, OrcidError.class, NotificationPermission.class);
        this.jaxbMarshaller = jaxbContext.createMarshaller();
        this.jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        this.httpClient = HttpClients.createDefault();
    }

    public String exchangeToken(String idToken, String orcidId) throws JSONException, IOException, DeactivatedException {
        HttpPost httpPost = new HttpPost(applicationProperties.getTokenExchange().getEndpoint());

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("client_id", applicationProperties.getTokenExchange().getClientId()));
        params.add(new BasicNameValuePair("client_secret", applicationProperties.getTokenExchange().getClientSecret()));
        params.add(new BasicNameValuePair("grant_type", applicationProperties.getTokenExchange().getGrantType()));
        params.add(new BasicNameValuePair("subject_token_type", applicationProperties.getTokenExchange().getSubjectTokenType()));
        params.add(new BasicNameValuePair("requested_token_type", applicationProperties.getTokenExchange().getRequestedTokenType()));
        params.add(new BasicNameValuePair("subject_token", idToken));
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        HttpResponse response = httpClient.execute(httpPost);
        Integer statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != Status.OK.getStatusCode()) {
            String responseString = EntityUtils.toString(response.getEntity());

            if (statusCode == Status.UNAUTHORIZED.getStatusCode() && responseString.contains("invalid_scope") && recordIsDeactivated(orcidId)) {
                LOG.info("Deactivated profile detected");
                throw new DeactivatedException();
            } else {
                LOG.error("Unable to exchange id_token for orcid ID {} : {}", orcidId, responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
        }

        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject json = new JSONObject(responseString);

        return json.get("access_token").toString();
    }

    public String postAffiliation(String orcid, String accessToken, Assertion assertion) throws DeprecatedException {
        Affiliation orcidAffiliation = AffiliationAdapter.toOrcidAffiliation(assertion);
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();
        LOG.info("Creating {} for {} with role title {}", affType, orcid, orcidAffiliation.getRoleTitle());

        HttpPost httpPost = new HttpPost(applicationProperties.getOrcidAPIEndpoint() + orcid + '/' + affType);
        setXmlHeaders(httpPost, accessToken);

        StringEntity entity = getStringEntity(orcidAffiliation);
        httpPost.setEntity(entity);

        try {
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 409) {
                throw new DeprecatedException();
            } else if (response.getStatusLine().getStatusCode() != Status.CREATED.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                LOG.error("Unable to create {} for {}. Status code: {}, error {}", affType, orcid, response.getStatusLine().getStatusCode(), responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
            String location = response.getFirstHeader("location").getValue();
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (IOException e) {
            LOG.error("Unable to create affiliation in ORCID", e);
        }
        return null;
    }

    public void putAffiliation(String orcid, String accessToken, Assertion assertion) throws DeprecatedException, IOException {
        Affiliation orcidAffiliation = AffiliationAdapter.toOrcidAffiliation(assertion);
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();
        LOG.info("Updating affiliation with put code {} for {}", assertion.getPutCode(), orcid);

        HttpPut httpPut = new HttpPut(applicationProperties.getOrcidAPIEndpoint() + orcid + '/' + affType + '/' + assertion.getPutCode());
        setXmlHeaders(httpPut, accessToken);

        StringEntity entity = getStringEntity(orcidAffiliation);
        httpPut.setEntity(entity);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() == 409) {
                throw new DeprecatedException();
            } else if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                LOG.error("Unable to update {} with putcode {} for {}. Status code: {}, error {}", affType, assertion.getPutCode(), orcid,
                    response.getStatusLine().getStatusCode(), responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
        } finally {
            response.close();
        }
    }

    public void deleteAffiliation(String orcid, String accessToken, Assertion assertion) throws IOException, DeprecatedException {
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();
        LOG.info("Deleting affiliation with putcode {} for {}", assertion.getPutCode(), orcid);

        HttpDelete httpDelete = new HttpDelete(applicationProperties.getOrcidAPIEndpoint() + orcid + '/' + affType + '/' + assertion.getPutCode());
        setXmlHeaders(httpDelete, accessToken);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpDelete);
            if (response.getStatusLine().getStatusCode() == 409) {
                throw new DeprecatedException();
            } else if (response.getStatusLine().getStatusCode() != Status.NO_CONTENT.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                LOG.error("Unable to delete {} with putcode {} for {}. Status code: {}, error {}", affType, assertion.getPutCode(), orcid,
                    response.getStatusLine().getStatusCode(), responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
        } finally {
            response.close();
        }
    }

    public String postNotification(NotificationPermission notificationPermission, String orcidId) throws JAXBException, IOException {
        return useInternalAccessToken(() -> {
            return postNotificationPermission(notificationPermission, orcidId);
        });
    }

    public String getOrcidIdForEmail(String email) throws IOException {
        return useInternalAccessToken(() -> {
            return getOrcidIdFromRegistry(email);
        });
    }

    public boolean recordIsDeactivated(String orcidId) {
        LOG.info("Checking to see if record {} is deactivated", orcidId);
        return useInternalAccessToken(() -> {
            return checkRegistryForDeactivated(orcidId);
        });
    }

    private boolean checkRegistryForDeactivated(String orcidId) {
        LOG.info("Calling {}/person endpoint to check deactivated status", orcidId);
        HttpGet httpGet = new HttpGet(applicationProperties.getOrcidAPIEndpoint() + orcidId + "/person");
        setJsonHeaders(httpGet, internalAccessToken);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            LOG.info("Received status {} from the registry", response.getStatusLine().getStatusCode());
            return response.getStatusLine().getStatusCode() == Status.CONFLICT.getStatusCode();
        } catch (Exception e) {
            LOG.error("Error checking registry for deactivated record {}", orcidId, e);
            throw new RuntimeException(e);
        }
    }

    private <T> T useInternalAccessToken(Supplier<T> function) {
        initInternalAccessToken();
        try {
            return function.get();
        } catch (Exception e) {
            LOG.info("Refreshing internal access token");
            createInternalAccessToken();
            return function.get();
        }
    }

    private void initInternalAccessToken() {
        if (internalAccessToken == null) {
            createInternalAccessToken();
        }
    }

    private void createInternalAccessToken() {
        try {
            internalAccessToken = getInternalAccessToken();
        } catch (Exception e) {
            LOG.error("Failed to create internal access token", e);
            throw new RuntimeException(e);
        }
    }

    private String postNotificationPermission(NotificationPermission notificationPermission, String orcidId) {
        HttpPost httpPost = new HttpPost(applicationProperties.getOrcidAPIEndpoint() + orcidId + "/notification-permission");
        setXmlHeaders(httpPost, internalAccessToken);

        StringEntity entity = getStringEntity(notificationPermission);
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != Status.CREATED.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                LOG.error("Unable to create notification for {}. Status code: {}, error {}", orcidId, response.getStatusLine().getStatusCode(), responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
            String location = response.getFirstHeader("location").getValue();
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (Exception e) {
            LOG.error("Error posting notification permission", e);
            throw new RuntimeException(e);
        }
    }

    private String getOrcidIdFromRegistry(String email) {
        HttpGet httpGet = new HttpGet(applicationProperties.getInternalRegistryApiEndpoint() + "orcid/" + Base64.encode(email) + "/email");
        setJsonHeaders(httpGet, internalAccessToken);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode() && response.getStatusLine().getStatusCode() != Status.NOT_FOUND.getStatusCode()) {
                LOG.warn("Received non-200 / non-404 response trying to find orcid id for email {}", email);
                String responseString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                LOG.warn("Response received:");
                LOG.warn(responseString);
                throw new RuntimeException("Received non-200 / non-404 response trying to find orcid id for email");
            } else if (response.getStatusLine().getStatusCode() != Status.NOT_FOUND.getStatusCode()) {
                Map<String, String> responseMap = new ObjectMapper().readValue(response.getEntity().getContent(), new TypeReference<HashMap<String, String>>() {
                });
                String orcidId = responseMap.get("orcid");
                if (!StringUtils.isBlank(orcidId)) {
                    return orcidId;
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting orcid id for {}", email, e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private String getInternalAccessToken() throws JSONException, ClientProtocolException, IOException {
        HttpPost httpPost = new HttpPost(applicationProperties.getInternalRegistryApiEndpoint() + "/oauth/token");

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("client_id", applicationProperties.getTokenExchange().getClientId()));
        params.add(new BasicNameValuePair("client_secret", applicationProperties.getTokenExchange().getClientSecret()));
        params.add(new BasicNameValuePair("scope", "/premium-notification /orcid-internal"));
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        HttpResponse response = httpClient.execute(httpPost);
        Integer statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != Status.OK.getStatusCode()) {
            String responseString = EntityUtils.toString(response.getEntity());
            LOG.error("Failed to obtain internal access token: {}", responseString);
            throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
        }

        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject json = new JSONObject(responseString);

        return json.get("access_token").toString();
    }

    private void setXmlHeaders(HttpRequestBase request, String accessToken) {
        request.setHeader(HttpHeaders.ACCEPT, "application/xml");
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private void setJsonHeaders(HttpRequestBase request, String accessToken) {
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private StringEntity getStringEntity(Object entity) {
        StringWriter sw = new StringWriter();
        try {
            jaxbMarshaller.marshal(entity, sw);
        } catch (JAXBException e) {
            LOG.error("Error marshalling string entity", e);
            throw new RuntimeException(e);
        }
        String xmlObject = sw.toString();
        return new StringEntity(xmlObject, ContentType.create("text/xml", Consts.UTF_8));
    }

}
