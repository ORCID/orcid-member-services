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
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.domain.User;
import org.orcid.mp.assertion.domain.adapter.AffiliationAdapter;
import org.orcid.mp.assertion.error.DeactivatedException;
import org.orcid.mp.assertion.error.DeprecatedException;
import org.orcid.mp.assertion.error.OrcidAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class OrcidApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrcidApiClient.class);

    private String internalAccessToken;

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

    public String exchangeToken(String idToken, String orcidId) throws IOException, DeactivatedException, JSONException {
        Map<String, String> params = Map.of("client_id", clientId, "client_secret",
                clientSecret, "grant_type", grantType, "subject_token_type",
                subjectTokenType, "requested_token_type", requestedTokenType, "subject_token", idToken);

        ResponseEntity<String> response = restClient.post().uri(tokenExchangeUrl).body(params).retrieve().toEntity(String.class);
        String responseString = response.getBody();
        HttpStatusCode statusCode = response.getStatusCode();

        if (!statusCode.is2xxSuccessful()) {
            if (statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED) && responseString.contains("invalid_scope") && recordIsDeactivated(orcidId)) {
                LOG.info("Deactivated profile detected");
                throw new DeactivatedException();
            } else {
                LOG.error("Unable to exchange id_token for orcid ID {} : {}", orcidId, responseString);
                throw new OrcidAPIException(statusCode.value(), responseString);
            }
        }

        JSONObject json = new JSONObject(responseString);
        return json.get("access_token").toString();
    }

    public String postAffiliation(String orcid, String accessToken, Assertion assertion) throws DeprecatedException, IOException {
        Affiliation orcidAffiliation = AffiliationAdapter.toOrcidAffiliation(assertion);
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();

        LOG.info("Creating {} for {} with role title {}", affType, orcid, orcidAffiliation.getRoleTitle());
        ResponseEntity<String> response = restClient.post()
                .uri(apiUrl + orcid + '/' + affType)
                .header("Authorization", "Bearer " + accessToken)
                .body(orcidAffiliation).retrieve().toEntity(String.class);

        String responseString = response.getBody();
        HttpStatusCode statusCode = response.getStatusCode();

        if (statusCode.isSameCodeAs(HttpStatus.CONFLICT)) {
            LOG.info("Detected deprecated profile {}", orcid);
            throw new DeprecatedException();
        } else if (!statusCode.isSameCodeAs(HttpStatus.CREATED)) {
            LOG.error("Unable to create {} for {}. Status code: {}, error {}", affType, orcid, statusCode.value(), responseString);
            throw new OrcidAPIException(statusCode.value(), responseString);
        }

        String location = response.getHeaders().getFirst("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public void putAffiliation(String orcid, String accessToken, Assertion assertion) throws DeprecatedException, IOException {
        Affiliation orcidAffiliation = AffiliationAdapter.toOrcidAffiliation(assertion);
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();

        LOG.info("Updating affiliation with put code {} for {}", assertion.getPutCode(), orcid);
        ResponseEntity<String> response = restClient.put()
                .uri(apiUrl + orcid + '/' + affType + '/' + assertion.getPutCode())
                .header("Authorization", "Bearer " + accessToken)
                .body(orcidAffiliation).retrieve().toEntity(String.class);

        String responseString = response.getBody();
        HttpStatusCode statusCode = response.getStatusCode();

        if (statusCode.isSameCodeAs(HttpStatus.CONFLICT)) {
            LOG.info("Detected deprecated profile {}", orcid);
            throw new DeprecatedException();
        } else if (!statusCode.isSameCodeAs(HttpStatus.OK)) {
            LOG.error("Unable to update {} for {}. Status code: {}, error {}", affType, orcid, statusCode.value(), responseString);
            throw new OrcidAPIException(statusCode.value(), responseString);
        }
    }

    public void deleteAffiliation(String orcid, String accessToken, Assertion assertion) throws IOException, DeprecatedException {
        String affType = assertion.getAffiliationSection().getOrcidEndpoint();

        LOG.info("Deleting affiliation with putcode {} for {}", assertion.getPutCode(), orcid);
        ResponseEntity<String> response = restClient.delete()
                .uri(apiUrl + orcid + '/' + affType + '/' + assertion.getPutCode())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().toEntity(String.class);

        String responseString = response.getBody();
        HttpStatusCode statusCode = response.getStatusCode();

        if (statusCode.isSameCodeAs(HttpStatus.CONFLICT)) {
            LOG.info("Detected deprecated profile {}", orcid);
            throw new DeprecatedException();
        } else if (!statusCode.isSameCodeAs(HttpStatus.NO_CONTENT)) {
            LOG.error("Unable to delete {} for {}. Status code: {}, error {}", affType, orcid, statusCode.value(), responseString);
            throw new OrcidAPIException(statusCode.value(), responseString);
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
        ResponseEntity<String> response = restClient.get().uri(apiUrl+ "/" + orcidId  + "/person").header("Authorization", "Bearer " + internalAccessToken).retrieve().toEntity(String.class);
        String responseString = response.getBody();

        LOG.info("Received status {} from the registry", response.getStatusCode().value());
        if (response.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw new OrcidAPIException(response.getStatusCode().value(), responseString);
        } else {
            return response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT);
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

    private String getInternalAccessToken() throws JSONException, IOException {
        Map<String, String> params = Map.of("client_id", clientId, "client_secret",
                clientSecret, "scope", "/premium-notification /orcid-internal",
                "grant_type", "client_credentials");

        ResponseEntity<String> response = restClient.post().uri(internalApiUrl + "/oauth/token").body(params).retrieve().toEntity(String.class);
        String responseString = response.getBody();
        HttpStatusCode statusCode = response.getStatusCode();

        if (!statusCode.is2xxSuccessful()) {
            LOG.error("Failed to obtain internal access token: {}", responseString);
            throw new OrcidAPIException(response.getStatusCode().value(), responseString);
        }

        JSONObject json = new JSONObject(responseString);
        return json.get("access_token").toString();
    }

    private String postNotificationPermission(NotificationPermission notificationPermission, String orcidId) {
        ResponseEntity<String> response = restClient.post().uri(apiUrl  + orcidId + "/notification-permission").header("Authorization", "Bearer " + internalAccessToken).body(notificationPermission).retrieve().toEntity(String.class);
        if (!response.getStatusCode().isSameCodeAs(HttpStatus.CREATED)) {
            String responseString = response.getBody();
            LOG.error("Unable to create notification for {}. Status code: {}, error {}", orcidId, response.getStatusCode().value(), responseString);
            throw new OrcidAPIException(response.getStatusCode().value(), responseString);
        }

        String location = response.getHeaders().getFirst("location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String getOrcidIdFromRegistry(String email) {
        ResponseEntity<String> response = restClient.get().uri(internalApiUrl+ "orcid/" + Base64.encode(email) + "/email")
                .header("Authorization", "Bearer " + internalAccessToken)
                .retrieve().toEntity(String.class);

        String responseBody = response.getBody();
        if (!response.getStatusCode().isSameCodeAs(HttpStatus.OK) && !response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            LOG.warn("Received non-200 / non-404 response trying to find orcid id for email {}", email);
            LOG.warn("Response received:");
            LOG.warn(responseBody);
            throw new RuntimeException("Received non-200 / non-404 response trying to find orcid id for email");
        } else if (!response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            try {
                Map<String, String> responseMap = new ObjectMapper().readValue(responseBody, new TypeReference<HashMap<String, String>>() {
                });
                String orcidId = responseMap.get("orcid");
                if (!StringUtils.isBlank(orcidId)) {
                    return orcidId;
                }
            } catch (JsonProcessingException e) {
                LOG.error("Error extracting orcid id from response: {}", responseBody, e);
                throw new RuntimeException(e);
            }
        }

        return null;
    }

}
