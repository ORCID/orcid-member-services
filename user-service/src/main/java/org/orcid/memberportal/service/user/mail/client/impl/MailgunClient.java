package org.orcid.memberportal.service.user.mail.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.orcid.memberportal.service.user.config.ApplicationProperties;
import org.orcid.memberportal.service.user.mail.MailException;
import org.orcid.memberportal.service.user.mail.client.MailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MailgunClient implements MailClient {

    private final Logger LOGGER = LoggerFactory.getLogger(MailgunClient.class);

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private HttpClient client;

    @Override
    public void sendMail(String to, String subject, String html) throws MailException {
        LOGGER.info("Preparing email {} for sending to {}", subject, to);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("to", to));
        urlParameters.add(new BasicNameValuePair("from", getFrom()));
        urlParameters.add(new BasicNameValuePair("subject", subject));
        urlParameters.add(new BasicNameValuePair("html", html));

        if (applicationProperties.isMailTestMode()) {
            urlParameters.add(new BasicNameValuePair("o:testmode", "yes"));
            LOGGER.info("Test mode email {} to {}", subject, to);
            LOGGER.info(html);
        }

        HttpPost post = new HttpPost(applicationProperties.getMailApiUrl());
        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters));
        } catch (UnsupportedEncodingException e) {
            throw new MailException("Error encoding url params for post body", e);
        }

        try {
            LOGGER.info("Sending mail {} to {}", subject, to);
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.warn("Received response {} from mailgun: {}", response.getStatusLine().getReasonPhrase());
                try (InputStream inputStream = response.getEntity().getContent()) {
                    LOGGER.warn(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                }
            } else {
                EntityUtils.consume(response.getEntity());
            }
        } catch (IOException e) {
            throw new MailException("Error posting mail to mailgun", e);
        }
    }

    private String getFrom() {
        return applicationProperties.getMailFromName() != null ? applicationProperties.getMailFromName() + " <" + applicationProperties.getMailFromAddress() + ">"
                : applicationProperties.getMailFromAddress();
    }

}
