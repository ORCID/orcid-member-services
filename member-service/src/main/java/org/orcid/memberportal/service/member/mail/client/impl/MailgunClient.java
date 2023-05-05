package org.orcid.memberportal.service.member.mail.client.impl;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import org.apache.http.util.EntityUtils;
import org.orcid.memberportal.service.member.mail.MailException;
import org.orcid.memberportal.service.member.mail.client.MailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class MailgunClient implements MailClient {

    private final Logger LOGGER = LoggerFactory.getLogger(MailgunClient.class);

    private HttpClient httpClient;

    private boolean testMode;

    private String mailApiUrl;

    private String fromName;

    private String fromAddress;

    @Override
    public void sendMail(String to, String subject, String html) throws MailException {
        LOGGER.info("Preparing email {} for sending to {} from {}", subject, to, getFrom());

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(StandardCharsets.UTF_8);
        builder.addTextBody("to", to);
        builder.addTextBody("from", getFrom());
        builder.addPart("subject", new StringBody(subject, ContentType.create("text/plain", StandardCharsets.UTF_8)));
        builder.addPart("html", new StringBody(html, ContentType.create("text/html", StandardCharsets.UTF_8)));

        if (testMode) {
            builder.addTextBody("o:testmode", "yes");
            LOGGER.info("Test mode email {} to {}", subject, to);
            LOGGER.info(html);
        } else {
            LOGGER.info("Sending mail {} to {}", subject, to);
            send(builder);
        }
    }

    @Override
    public void sendMailWithAttachment(String to, String subject, String html, File file) throws MailException {
        LOGGER.info("Preparing email {} for sending to {} from {}", subject, to, getFrom());

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(StandardCharsets.UTF_8);
        builder.addTextBody("to", to);
        builder.addTextBody("from", getFrom());
        builder.addPart("subject", new StringBody(subject, ContentType.create("text/plain", StandardCharsets.UTF_8)));
        builder.addPart("html", new StringBody(html, ContentType.create("text/html", StandardCharsets.UTF_8)));
        builder.addPart("attachment", new FileBody(file));

        if (testMode) {
            builder.addTextBody("o:testmode", "yes");
            LOGGER.info("Test mode email {} with attachment {} to {}", subject, file.getName(), to);
            LOGGER.info(html);
        } else {
            LOGGER.info("Sending mail {} to {}", subject, to);
            send(builder);
        }
    }

    private void send(MultipartEntityBuilder builder) throws MailException {
        HttpPost post = new HttpPost(mailApiUrl);
        post.setEntity(builder.build());

        try {
            HttpResponse response = httpClient.execute(post);
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

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
