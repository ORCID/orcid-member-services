package org.orcid.mp.member.client;

import java.util.Map;

import org.orcid.mp.member.error.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MailgunClient {

    private final Logger LOGGER = LoggerFactory.getLogger(MailgunClient.class);

    @Value("${application.mail.fromAddress}")
    private String mailFromAddress;

    @Value("${application.mail.fromName}")
    private String mailFromName;

    @Value("${application.mail.testMode}")
    private boolean testMode;

    private RestClient client;

    public MailgunClient(@Qualifier("mailgunRestClient") RestClient client) {
        this.client = client;
    }

    public void sendMail(String to, String subject, String html) throws MailException {
        sendMail(to, getFrom(), subject, html);
    }

    public void sendMail(String to, String from, String subject, String html) throws MailException {
        LOGGER.info("Preparing email {} for sending to {} from {}", subject, to, from);
        Map<String, String> formData = Map.of("from", from, "to", to, "subject", subject, "html", html);

        if (testMode) {
            formData.put("o:testmode", "yes");
            LOGGER.info("Test mode email {} to {}", subject, to);
            LOGGER.info(html);
        }

        try {
            ResponseEntity<String> response = client.post().body(formData).retrieve().toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                LOGGER.warn("Received response from mailgun {} - {}", response.getStatusCode().value(), response.getBody());
            }
        } catch (Exception e) {
            throw new MailException("Error posting mail to mailgun", e);
        }
    }

    private String getFrom() {
        return mailFromName != null ? mailFromName + " <" + mailFromAddress + ">" : mailFromAddress;
    }

}