package org.orcid.memberportal.service.assertion.client;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private ApplicationProperties applicationProperties;

    public OrcidAPIClient() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Affiliation.class, Distinction.class, Employment.class, Education.class, InvitedPosition.class,
                Membership.class, Qualification.class, Service.class, OrcidError.class, NotificationPermission.class);
        this.jaxbMarshaller = jaxbContext.createMarshaller();
        this.jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        this.httpClient = HttpClients.createDefault();
    }

    public String exchangeToken(String idToken) throws JSONException, ClientProtocolException, IOException {
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
            LOG.error("Unable to exchange id_token: {}", responseString);
            throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
        }

        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject json = new JSONObject(responseString);

        return json.get("access_token").toString();
    }

    public String postAffiliation(String orcid, String accessToken, Assertion assertion) throws JAXBException {
        Affiliation orcidAffiliation = AffiliationAdapter.toOrcidAffiliation(assertion);
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();
        LOG.info("Creating {} for {} with role title {}", affType, orcid, orcidAffiliation.getRoleTitle());

        HttpPost httpPost = new HttpPost(applicationProperties.getOrcidAPIEndpoint() + orcid + '/' + affType);
        setXmlHeaders(httpPost, accessToken);

        StringEntity entity = getStringEntity(orcidAffiliation);
        httpPost.setEntity(entity);

        try {
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != Status.CREATED.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                LOG.error("Unable to create {} for {}. Status code: {}, error {}", affType, orcid, response.getStatusLine().getStatusCode(), responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
            String location = response.getFirstHeader("location").getValue();
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (ClientProtocolException e) {
            LOG.error("Unable to create affiliation in ORCID", e);
        } catch (IOException e) {
            LOG.error("Unable to create affiliation in ORCID", e);
        }
        return null;
    }

    public void putAffiliation(String orcid, String accessToken, Assertion assertion) throws JAXBException, IOException {
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
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                LOG.error("Unable to update {} with putcode {} for {}. Status code: {}, error {}", affType, assertion.getPutCode(), orcid,
                        response.getStatusLine().getStatusCode(), responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
        } finally {
            response.close();
        }
    }

    public void deleteAffiliation(String orcid, String accessToken, Assertion assertion) throws IOException {
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();
        LOG.info("Deleting affiliation with putcode {} for {}", assertion.getPutCode(), orcid);

        HttpDelete httpDelete = new HttpDelete(applicationProperties.getOrcidAPIEndpoint() + orcid + '/' + affType + '/' + assertion.getPutCode());
        setXmlHeaders(httpDelete, accessToken);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpDelete);
            if (response.getStatusLine().getStatusCode() != Status.NO_CONTENT.getStatusCode()) {
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
        HttpPost httpPost = new HttpPost(applicationProperties.getOrcidAPIEndpoint() + orcidId + "/notification-permission");
        setXmlHeaders(httpPost, applicationProperties.getInternalRegistryAccessToken());

        StringEntity entity = getStringEntity(notificationPermission);
        httpPost.setEntity(entity);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != Status.CREATED.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                LOG.error("Unable to create notification for {}. Status code: {}, error {}", orcidId, response.getStatusLine().getStatusCode(), responseString);
                throw new ORCIDAPIException(response.getStatusLine().getStatusCode(), responseString);
            }
            String location = response.getFirstHeader("location").getValue();
            return location.substring(location.lastIndexOf('/') + 1);
        } finally {
            response.close();
        }
    }

    public String getOrcidIdForEmail(String email) throws IOException {
        HttpGet httpGet = new HttpGet(applicationProperties.getInternalRegistryApiEndpoint() + "orcid/" + Base64.encode(email) + "/email");
        setJsonHeaders(httpGet, applicationProperties.getInternalRegistryAccessToken());

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                LOG.warn("Received non-200 response trying to find orcid id for email {}", email);
                String responseString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                LOG.warn("Response received:");
                LOG.warn(responseString);
            } else {
                Map<String, String> responseMap = new ObjectMapper().readValue(response.getEntity().getContent(), new TypeReference<HashMap<String, String>>() {
                });
                String orcidId = responseMap.get("orcid");
                if (!StringUtils.isBlank(orcidId)) {
                    return orcidId;
                }
            }
        } finally {
            response.close();
        }
        return null;
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

    private StringEntity getStringEntity(Object entity) throws JAXBException {
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(entity, sw);
        String xmlObject = sw.toString();
        return new StringEntity(xmlObject, ContentType.create("text/xml", Consts.UTF_8));
    }

}
