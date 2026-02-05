package org.orcid.mp.assertion.client;

import org.orcid.mp.assertion.error.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class MailgunClient {

    private final Logger LOGGER = LoggerFactory.getLogger(MailgunClient.class);

    @Value("${application.mail.fromAddress}")
    private String mailFromAddress;

    @Value("${application.mail.fromName}")
    private String mailFromName;

    @Value("${application.mail.testMode}")
    private boolean testMode;

    private final RestClient client;

    public MailgunClient(@Qualifier("mailgunRestClient") RestClient client) {
        this.client = client;
    }

    public void sendMail(String to, String subject, String html) throws MailException {
        sendMail(to, getFrom(), subject, html);
    }

    public void sendMail(String to, String from, String subject, String html) throws MailException {
        LOGGER.info("Preparing email {} for sending to {} from {}", subject, to, from);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("from", from);
        formData.add("to", to);
        formData.add("subject", subject);
        formData.add("html", html);

        if (testMode) {
            formData.add("o:testmode", "yes");
            LOGGER.info("Test mode email {} to {}", subject, to);
        }

        try {
            ResponseEntity<String> response = client.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                LOGGER.warn("Received response from mailgun {} - {}", response.getStatusCode().value(), response.getBody());
            }
        } catch (Exception e) {
            throw new MailException("Error posting mail to mailgun", e);
        }
    }

    public void sendMailWithAttachment(String to, String subject, String html, File file) throws MailException {
        LOGGER.info("Preparing email {} for sending to {} from {}", subject, to, getFrom());

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("from", getFrom());
        formData.add("to", to);
        formData.add("subject", subject);
        formData.add("html", html);
        formData.add("attachment", new FileSystemResource(file));

        if (testMode) {
            formData.add("o:testmode", "yes");
            LOGGER.info("Test mode email {} with attachment {} to {}", subject, file.getName(), to);
        }

        try {
            ResponseEntity<String> response = client.post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formData)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                LOGGER.warn("Received response from mailgun {} - {}", response.getStatusCode().value(), response.getBody());
            }
        } catch (Exception e) {
            throw new MailException("Error posting mail with attachment to mailgun", e);
        }
    }

    private String getFrom() {
        return mailFromName != null ? mailFromName + " <" + mailFromAddress + ">" : mailFromAddress;
    }

}
