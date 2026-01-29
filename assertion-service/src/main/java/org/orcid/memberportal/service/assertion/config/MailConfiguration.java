package org.orcid.memberportal.service.assertion.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.orcid.memberportal.service.assertion.mail.client.impl.MailgunClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MailConfiguration.class);

    @Bean
    public HttpClient mailgunHttpClient(ApplicationProperties applicationProperties) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("api", applicationProperties.getMailApiKey());
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);

        HttpRequestRetryHandler retryHandler = (exception, executionCount, context) -> {
            if (executionCount > 3) {
                LOG.warn("Retry limit exceeded, email will not be sent");
                return false;
            }
            if (exception instanceof java.net.SocketException) {
                LOG.warn("Detected connection reset - retrying sending email...");
                return true;
            }
            return false;
        };

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setValidateAfterInactivity(2000);

        HttpClient client = HttpClientBuilder.create()
            .setDefaultCredentialsProvider(provider)
            .setRetryHandler(retryHandler)
            .setConnectionManager(connectionManager)
            .evictExpiredConnections()
            .evictIdleConnections(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        return client;
    }

    @Bean
    public MailgunClient mailgunClient(HttpClient httpClient, ApplicationProperties applicationProperties) {
        MailgunClient mailgunClient = new MailgunClient();
        mailgunClient.setFromAddress(applicationProperties.getMailFromAddress());
        mailgunClient.setFromName(applicationProperties.getMailFromName());
        mailgunClient.setMailApiUrl(applicationProperties.getMailApiUrl());
        mailgunClient.setTestMode(applicationProperties.isMailTestMode());
        mailgunClient.setHttpClient(httpClient);
        return mailgunClient;
    }

}
