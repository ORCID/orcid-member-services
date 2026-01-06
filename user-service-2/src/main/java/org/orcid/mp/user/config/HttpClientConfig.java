package org.orcid.mp.user.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Configuration
public class HttpClientConfig {

    @Value("${application.httpclient.maxConnTotal}")
    private int maxConnTotal;

    @Value("${application.httpclient.maxConnPerRoute}")
    private int maxConnPerRoute;

    @Value("${application.httpclient.connectionTimeToLive}")
    private int connectionTimeToLive;

    @Value("${application.mail.apiKey}")
    private String mailApiKey;

    @Value("${application.mail.apiUrl}")
    private String mailApiUrl;

    @Bean(name = "mailgunRestClient")
    public RestClient mailgunRestClient() {
        CloseableHttpClient httpClient = getCloseableHttpClient();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().baseUrl(mailApiUrl).defaultHeader("Authorization", "Basic " + getEncodedMailgunCredentials()).requestFactory(requestFactory).build();
    }

    @Bean(name = "memberServiceRestClient")
    public RestClient memberServiceRestClient() {
        CloseableHttpClient httpClient = getCloseableHttpClient();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().requestInterceptor(new BearerTokenInterceptor()).requestFactory(requestFactory).build();
    }

    private CloseableHttpClient getCloseableHttpClient() {
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(maxConnTotal);
        poolingConnManager.setDefaultMaxPerRoute(maxConnPerRoute);
        poolingConnManager.setValidateAfterInactivity(TimeValue.ofMilliseconds(1000));

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(poolingConnManager)
                .evictIdleConnections(TimeValue.ofSeconds(connectionTimeToLive))
                .build();
        return httpClient;
    }

    private String getEncodedMailgunCredentials() {
        String auth = "api" + ":" + mailApiKey;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }

}
