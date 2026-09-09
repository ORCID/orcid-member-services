package org.orcid.mp.member.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public class ApiCredentialsServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiCredentialsServiceClient.class);

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${application.apicreds.apiBaseUrl}")
    private String apiBaseUrl;

    @Value("${application.apicreds.clientId}")
    private String clientId;

    @Value("${application.apicreds.clientSecret}")
    private String clientSecret;

    @Autowired
    @Qualifier("apiCredentialsServiceRestClient")
    private RestClient restClient;

    public String getOrcidRegistryApiClients(String memberId) {
        LOG.debug("Fetching example data from new API...");
        return request(() -> getApiClients(memberId));
    }

    private String getApiClients(String memberId) {
        String url = apiBaseUrl + "/" + memberId;

        LOG.debug("Sending GET request to API Credentials service: {}", url);
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken.get()))
                    .retrieve()
                    .toEntity(String.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw ex;
            }

            LOG.warn("Received non-2xx response from API Credentials service at {}", url, ex);
            LOG.info("Response code is {}", ex.getStatusCode());
            LOG.info("Response body is {}", ex.getResponseBodyAsString());
            return null;
        }
    }
    private <T> T request(Supplier<T> function) {
        initAccessToken();
        try {
            return function.get();
        } catch (Exception e) {
            LOG.debug("Exception after API request, attempting token refresh", e);
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
        LOG.debug("Acquiring access token...");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("scope", "data.read data.write");

        try {
            String responseString = restClient.post().uri(apiBaseUrl + "/oauth2/token")
                    .contentType(new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8))
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            LOG.debug("Access token acquired successfully");
            JsonNode responseJson = objectMapper.readTree(responseString);
            return responseJson.get("access_token").textValue();
        } catch (Exception ex) {
            LOG.error("Failed to acquire access token: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error while acquiring access token", ex);
        }
    }
}