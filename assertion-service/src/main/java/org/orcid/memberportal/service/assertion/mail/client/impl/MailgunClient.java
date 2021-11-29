package org.orcid.memberportal.service.assertion.mail.client.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;
import org.orcid.memberportal.service.assertion.mail.MailException;
import org.orcid.memberportal.service.assertion.mail.client.MailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MailgunClient implements MailClient {

    private final Logger LOGGER = LoggerFactory.getLogger(MailgunClient.class);

    @Autowired
    private HttpClient client;

    private boolean testMode;

    private String mailApiUrl;

    private String fromName;

    private String fromAddress;

    @Override
    public void sendMailWithAttachment(String to, String subject, String html, File file) throws MailException {
        LOGGER.info("Preparing email {} for sending to {}", subject, to);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("to", to);
        builder.addTextBody("from", getFrom());
        builder.addTextBody("subject", subject);
        builder.addTextBody("html", html);
        builder.addPart("attachment", new FileBody(file));

        if (testMode) {
            builder.addTextBody("o:testmode", "yes");
            LOGGER.info("Test mode email {} to {}", subject, to);
            LOGGER.info(html);
        }

        HttpPost post = new HttpPost(mailApiUrl);
        post.setEntity(builder.build());

        try {
            LOGGER.info("Sending mail {} to {}", subject, to);
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.warn("Received response {} from mailgun", response.getStatusLine().getReasonPhrase());
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
        return fromName != null ? fromName + " <" + fromAddress + ">" : fromAddress;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public void setMailApiUrl(String mailApiUrl) {
        this.mailApiUrl = mailApiUrl;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

}
