package org.orcid.memberportal.service.member.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.orcid.memberportal.service.member.mail.client.impl.MailgunClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfiguration {

    @Bean
    public HttpClient mailgunHttpClient(ApplicationProperties applicationProperties) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("api", applicationProperties.getMailApiKey());

        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);

        HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
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
