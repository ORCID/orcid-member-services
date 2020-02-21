package org.orcid.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.orcid.config.ApplicationProperties;
import org.orcid.domain.Assertion;
import org.orcid.domain.adapter.OrcidAffiliationAdapter;
import org.orcid.jaxb.model.v3.release.error.OrcidError;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.jaxb.model.v3.release.record.Distinction;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.InvitedPosition;
import org.orcid.jaxb.model.v3.release.record.Membership;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.Service;
import org.orcid.web.rest.AssertionServicesResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.stereotype.Component;

@Component
public class OrcidAPIClient {
    private final Logger log = LoggerFactory.getLogger(AssertionServicesResource.class);

    @Autowired
    private ApplicationProperties applicationProperties;

    public String exchangeToken(String idToken) throws JSONException, ClientProtocolException, IOException, JAXBException {

        HttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(applicationProperties.getTokenExchange().getEndpoint());

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("client_id", applicationProperties.getTokenExchange().getClientId()));
        params.add(new BasicNameValuePair("client_secret", applicationProperties.getTokenExchange().getClientSecret()));
        params.add(new BasicNameValuePair("grant_type", applicationProperties.getTokenExchange().getGrantType()));
        params.add(new BasicNameValuePair("subject_token_type", applicationProperties.getTokenExchange().getSubjectTokenType()));
        params.add(new BasicNameValuePair("requested_token_type", applicationProperties.getTokenExchange().getRequestedTokenType()));
        params.add(new BasicNameValuePair("subject_token", idToken));
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        HttpResponse response = client.execute(httpPost);
        Integer statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != Status.OK.getStatusCode()) {
            String responseString = EntityUtils.toString(response.getEntity());
            log.error("Unable to exchange id_token: {}", responseString);
            checkAndHandleInvalidTokenException(response, responseString);
            throw new IllegalArgumentException("Unable to exchange id_token, status code: " + statusCode);
        }

        String responseString = EntityUtils.toString(response.getEntity());        
        JSONObject json = new JSONObject(responseString);

        return json.get("access_token").toString();
    }

    public String postAffiliation(String orcid, String accessToken, Assertion assertion) throws JAXBException {
        Affiliation orcidAffiliation = OrcidAffiliationAdapter.toOrcidAffiliation(assertion);
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();
        log.info("Creating {} for {} with role title {}", affType, orcid, orcidAffiliation.getRoleTitle());

        HttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(applicationProperties.getOrcidAPIEndpoint() + orcid + '/' + affType);
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/xml");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        
        JAXBContext jaxbContext = JAXBContext.newInstance(Affiliation.class, Distinction.class, Employment.class, Education.class, InvitedPosition.class, Membership.class, Qualification.class, Service.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // To format XML

        //Print XML String to Console
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(orcidAffiliation, sw);
        String xmlObject = sw.toString();
        StringEntity entity = new StringEntity(xmlObject, ContentType.create("text/xml", Consts.UTF_8));

        httpPost.setEntity(entity);

        try {
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != Status.CREATED.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                log.error("Unable to create {} for {}. Status code: {}, error {}", affType, orcid, response.getStatusLine().getStatusCode(), responseString);
                checkAndHandleInvalidTokenException(response, responseString);
                throw new RuntimeException(responseString);
            }
            String location = response.getFirstHeader("location").getValue();
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (ClientProtocolException e) {
            log.error("Unable to push affiliation to ORCID", e);
        } catch (IOException e) {
            log.error("Unable to push affiliation to ORCID", e);
        }
        return null;
    }

    public String putAffiliation(String orcid, String accessToken, Assertion assertion) throws JSONException {
        // TODO
        return null;
    }

    public boolean deleteAffiliation(String orcid, String accessToken, Assertion assertion) throws JSONException {
        // TODO
        return false;
    }

    private void checkAndHandleInvalidTokenException(HttpResponse response, String responseString) throws JSONException, JAXBException {
        if (response.getStatusLine().getStatusCode() == Status.BAD_REQUEST.getStatusCode()
                || response.getStatusLine().getStatusCode() == Status.UNAUTHORIZED.getStatusCode()) {
            JAXBContext jaxbContext = JAXBContext.newInstance(Affiliation.class, Distinction.class, Employment.class, Education.class, InvitedPosition.class, Membership.class, Qualification.class, Service.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();  

            OrcidError error = (OrcidError) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(responseString.getBytes()));
            JSONObject obj = new JSONObject(responseString);
            if (obj.has("error") && (obj.getString("error").equals("invalid_scope") || obj.getString("error").equals("invalid_token"))) {
                throw new InvalidTokenException("id_token is no longer valid");
            }
        }
    }
}
