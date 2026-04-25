package org.orcid.mp.assertion.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.mp.assertion.config.CacheConfig;
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.domain.adapter.AffiliationAdapter;
import org.orcid.mp.assertion.error.DeactivatedException;
import org.orcid.mp.assertion.error.DeprecatedException;
import org.orcid.mp.assertion.error.OrcidAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public class OrcidApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrcidApiClient.class);

    private final AtomicReference<String> internalAccessToken = new AtomicReference<>();

    @Autowired
    @Qualifier("orcidRestClient")
    private RestClient restClient;

    @Value("${application.orcid.apiUrl}")
    private String apiUrl;

    @Value("${application.orcid.internalApiUrl}")
    private String internalApiUrl;

    @Value("${application.orcid.tokenExchangeUrl}")
    private String tokenExchangeUrl;

    @Value("${application.orcid.clientId}")
    private String clientId;

    @Value("${application.orcid.clientSecret}")
    private String clientSecret;

    @Value("${application.orcid.grantType}")
    private String grantType;

    @Value("${application.orcid.subjectTokenType}")
    private String subjectTokenType;

    @Value("${application.orcid.requestedTokenType}")
    private String requestedTokenType;

    @Cacheable(value = CacheConfig.TOKEN_CACHE, key = "#idToken")
    public String exchangeToken(String idToken, String orcidId) throws DeactivatedException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", grantType);
        formData.add("subject_token_type", subjectTokenType);
        formData.add("requested_token_type", requestedTokenType);
        formData.add("subject_token", idToken);

        LOG.debug("Exchanging ID token for ORCID ID {}. Token: {}", orcidId, idToken);

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(tokenExchangeUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toEntity(String.class);

            String responseString = response.getBody();
            LOG.debug("Token exchange response: {} - {}", response.getStatusCode().value(), responseString);

            JSONObject json = new JSONObject(responseString);
            return json.get("access_token").toString();

        } catch (RestClientResponseException e) {
            String responseString = e.getResponseBodyAsString();
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED) && responseString.contains("invalid_scope") && recordIsDeactivated(orcidId)) {
                LOG.info("Deactivated profile detected");
                throw new DeactivatedException();
            } else {
                LOG.error("Unable to exchange id_token for orcid ID {} : {}", orcidId, responseString);
                throw new OrcidAPIException(statusCode.value(), responseString);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Unable to parse response from token exchange", e);
        }
    }

    public String postAffiliation(String orcid, String accessToken, Assertion assertion) throws DeprecatedException, JAXBException {
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();

        LOG.info("Creating {} for {} with role title {}", affType, orcid, assertion.getRoleTitle());
        String xmlPayload = marshalAssertion(assertion);

        LOG.debug("Post affiliation payload: {}", xmlPayload);
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(apiUrl + orcid + '/' + affType)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xmlPayload).retrieve().toEntity(String.class);

            LOG.debug("Post affiliation successful");
            String location = response.getHeaders().getFirst("Location");
            LOG.debug("Location header in response is {}", location);

            String putCode = location.substring(location.lastIndexOf('/') + 1);

            LOG.debug("Put code is {}", putCode);
            return putCode;
        } catch (RestClientResponseException e) {
            String responseString = e.getResponseBodyAsString();
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.CONFLICT)) {
                LOG.info("Detected deprecated profile {}", orcid);
                throw new DeprecatedException();
            } else {
                LOG.error("Unable to create {} for {}. Status code: {}, error {}", affType, orcid, statusCode.value(), responseString);
                throw new OrcidAPIException(statusCode.value(), responseString);
            }
        }
    }

    public void putAffiliation(String orcid, String accessToken, Assertion assertion) throws DeprecatedException, IOException, JAXBException {
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();

        LOG.info("Updating affiliation with put code {} for {}", assertion.getPutCode(), orcid);
        String xmlPayload = marshalAssertion(assertion);

        LOG.debug("Put affiliation payload: {}", xmlPayload);
        try {
            ResponseEntity<String> response = restClient.put()
                    .uri(apiUrl + orcid + '/' + affType + '/' + assertion.getPutCode())
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xmlPayload).retrieve().toEntity(String.class);

            LOG.debug("Put affiliation successful");
        } catch (RestClientResponseException e) {
            String responseString = e.getResponseBodyAsString();
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.CONFLICT)) {
                LOG.info("Detected deprecated profile {}", orcid);
                throw new DeprecatedException();
            } else if (!statusCode.isSameCodeAs(HttpStatus.OK)) {
                LOG.error("Unable to update {} for {}. Status code: {}, error {}", affType, orcid, statusCode.value(), responseString);
                throw new OrcidAPIException(statusCode.value(), responseString);
            }
        }
    }

    public void deleteAffiliation(String orcid, String accessToken, Assertion assertion) throws IOException, DeprecatedException {
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();

        LOG.info("Deleting affiliation with putcode {} for {}", assertion.getPutCode(), orcid);
        try {
            ResponseEntity<String> response = restClient.delete()
                    .uri(apiUrl + orcid + '/' + affType + '/' + assertion.getPutCode())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve().toEntity(String.class);
            LOG.debug("Delete affiliation successful");
        } catch (RestClientResponseException e) {
            String responseString = e.getResponseBodyAsString();
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.CONFLICT)) {
                LOG.info("Detected deprecated profile {}", orcid);
                throw new DeprecatedException();
            } else {
                LOG.error("Unable to delete {} for {}. Status code: {}, error {}", affType, orcid, statusCode.value(), responseString);
                throw new OrcidAPIException(statusCode.value(), responseString);
            }
        }
    }

    private String marshalAssertion(Assertion assertion) throws JAXBException {
        Affiliation orcidAffiliation = AffiliationAdapter.toOrcidAffiliation(assertion);
        JAXBContext context = JAXBContext.newInstance(orcidAffiliation.getClass());
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(orcidAffiliation, writer);
        return writer.toString();
    }

    public String postNotification(NotificationPermission notificationPermission, String orcidId) throws JAXBException {
        return useInternalAccessToken(() -> {
            return postNotificationPermission(notificationPermission, orcidId);
        });
    }

    public String getOrcidIdForEmail(String email) {
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
        try {
            ResponseEntity<String> response = restClient.get().uri(apiUrl + "/" + orcidId + "/person")
                    .header("Authorization", "Bearer " + internalAccessToken.get())
                    .retrieve().toEntity(String.class);

            // no exception thrown: 2xx reply, so profile isn't deactivated
            LOG.debug("{} is not deactivated", orcidId);
            return false;
        } catch (RestClientResponseException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            String responseString = e.getResponseBodyAsString();

            LOG.info("Received status {} from the registry", statusCode.value());
            if (statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                throw new OrcidAPIException(statusCode.value(), responseString);
            } else {
                return statusCode.isSameCodeAs(HttpStatus.CONFLICT);
            }
        }
    }

    private <T> T useInternalAccessToken(Supplier<T> function) {
        initInternalAccessToken();
        try {
            return function.get();
        } catch (Exception e) {
            LOG.info("Refreshing internal access token");
            synchronized (this) {
                createInternalAccessToken();
            }
            return function.get();
        }
    }

    private void initInternalAccessToken() {
        if (internalAccessToken.get() == null) {
            synchronized (this) {
                if (internalAccessToken.get() == null) {
                    createInternalAccessToken();
                }
            }
        }
    }

    private void createInternalAccessToken() {
        try {
            internalAccessToken.set(getInternalAccessToken());
        } catch (Exception e) {
            LOG.error("Failed to create internal access token", e);
            throw new RuntimeException(e);
        }
    }

    private String getInternalAccessToken() throws JSONException, IOException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("scope", "/premium-notification /orcid-internal");
        formData.add("grant_type", "client_credentials");

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(internalApiUrl + "/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toEntity(String.class);

            String responseString = response.getBody();
            JSONObject json = new JSONObject(responseString);
            return json.get("access_token").toString();

        } catch (RestClientResponseException e) {
            String responseString = e.getResponseBodyAsString();
            LOG.error("Failed to obtain internal access token. Status: {}, Response: {}", e.getStatusCode().value(), responseString);
            throw new OrcidAPIException(e.getStatusCode().value(), responseString);
        }
    }

    private String postNotificationPermission(NotificationPermission notificationPermission, String orcidId) {
        LOG.debug("Posting notification permission for {}. Payload: {}", orcidId, notificationPermission);

        try {
            JAXBContext context = JAXBContext.newInstance(NotificationPermission.class);
            Marshaller marshaller = context.createMarshaller();
            StringWriter writer = new StringWriter();
            marshaller.marshal(notificationPermission, writer);
            String xmlPayload = writer.toString();

            try {
                ResponseEntity<String> response = restClient.post().uri(apiUrl + orcidId + "/notification-permission")
                        .header("Authorization", "Bearer " + internalAccessToken.get())
                        .contentType(MediaType.APPLICATION_XML)
                        .body(xmlPayload).retrieve().toEntity(String.class);

                LOG.debug("Post notification permission successful");
                String location = response.getHeaders().getFirst("location");
                return location.substring(location.lastIndexOf('/') + 1);
            } catch (RestClientResponseException e) {
                HttpStatusCode statusCode = e.getStatusCode();
                String responseString = e.getResponseBodyAsString();
                LOG.error("Unable to create notification for {}. Status code: {}, error {}", orcidId, statusCode.value(), responseString);
                throw new OrcidAPIException(statusCode.value(), responseString);
            }
        } catch (JAXBException e) {
            LOG.error("Failed to marshal NotificationPermission for ORCID ID: {}", orcidId, e);
            throw new RuntimeException("Failed to marshal NotificationPermission to XML", e);
        }
    }

    private String getOrcidIdFromRegistry(String email) {
        LOG.debug("Looking up ORCID ID for email: {}", email);
        try {
            ResponseEntity<String> response = restClient.get().uri(internalApiUrl + "orcid/" + Base64.encode(email) + "/email")
                    .header("Authorization", "Bearer " + internalAccessToken.get())
                    .retrieve().toEntity(String.class);
            try {
                Map<String, String> responseMap = new ObjectMapper().readValue(response.getBody(), new TypeReference<HashMap<String, String>>() {
                });
                String orcidId = responseMap.get("orcid");
                if (!StringUtils.isBlank(orcidId)) {
                    LOG.debug("Found ORCID ID {} for email {}", orcidId, email);
                    return orcidId;
                }
            } catch (JsonProcessingException e) {
                LOG.error("Error extracting orcid id from response: {}", response, e);
                throw new RuntimeException(e);
            }
        } catch (RestClientResponseException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            String responseString = e.getResponseBodyAsString();

            LOG.debug("Get ORCID ID from registry response: {} - {}", statusCode.value(), responseString);
            if (!statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                LOG.warn("Received non-200 / non-404 response trying to find orcid id for email {}", email);
                LOG.warn("Response received:");
                LOG.warn(responseString);
                throw new RuntimeException("Received non-200 / non-404 response trying to find orcid id for email");
            }
        }
        LOG.debug("No ORCID ID found for email {}", email);
        return null;
    }
}
