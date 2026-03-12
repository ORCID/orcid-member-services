package org.orcid.mp.member.config;


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

    @Value("${application.userService.apiUrl}")
    private String userServiceApiUrl;

    @Bean(name = "mailgunRestClient")
    public RestClient mailgunRestClient(CloseableHttpClient httpClient) {
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().baseUrl(mailApiUrl).defaultHeader("Authorization", "Basic " + getEncodedMailgunCredentials()).requestFactory(requestFactory).build();
    }

    @Bean(name = "userServiceRestClient")
    public RestClient userServiceRestClient(CloseableHttpClient httpClient) {
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().baseUrl(userServiceApiUrl).requestInterceptor(new BearerTokenInterceptor()).requestFactory(requestFactory).build();
    }

    @Bean(name = "salesforceRestClient")
    public RestClient salesforceRestClient(CloseableHttpClient httpClient) {
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    public PoolingHttpClientConnectionManager getConnectionManager() {
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(maxConnTotal);
        poolingConnManager.setDefaultMaxPerRoute(maxConnPerRoute);
        poolingConnManager.setValidateAfterInactivity(TimeValue.ofMilliseconds(1000));
        return poolingConnManager;
    }

    @Bean
    public CloseableHttpClient getCloseableHttpClient(PoolingHttpClientConnectionManager poolingConnManager) {
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(poolingConnManager).evictExpiredConnections().evictIdleConnections(TimeValue.ofSeconds(connectionTimeToLive)).build();
        return httpClient;
    }

    private String getEncodedMailgunCredentials() {
        String auth = "api" + ":" + mailApiKey;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }

}