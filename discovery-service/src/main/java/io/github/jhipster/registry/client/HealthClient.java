package io.github.jhipster.registry.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.Response.Status;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.jhipster.registry.health.HealthStatus;
import io.github.jhipster.registry.health.Health;

@Component
public class HealthClient {

    private final Logger LOG = LoggerFactory.getLogger(HealthClient.class);

    private CloseableHttpClient httpClient;

    public HealthClient() {
        this.httpClient = HttpClients.createDefault();
    }

    public Health getHealth(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                LOG.warn("Received non-200 response trying to get health from url {}", url);
                if (response.getEntity() != null) {
                    String responseString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                    LOG.warn("Response received:");
                    LOG.warn(responseString);
                }
                Health health = new Health();
                health.setStatus(HealthStatus.UNKNOWN);
                return health;
            } else {
                Health health = new ObjectMapper().readValue(response.getEntity().getContent(), Health.class);
                return health;
            }
        } finally {
            if (response != null) {
                response.close();
            } else {
                LOG.warn("Network error getting health from {}", url);
            }
        }
    }

}
