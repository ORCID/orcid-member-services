package org.orcid.memberportal.service.assertion.services;

import java.io.File;
import java.util.Locale;

import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.mail.MailException;
import org.orcid.memberportal.service.assertion.mail.client.impl.MailgunClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

/**
 * Service for sending emails.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
@Async
public class MailService {

    private final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private SpringTemplateEngine templateEngine;

    private ApplicationProperties applicationProperties;

    private MailgunClient mailgunClient;

    public MailService(ApplicationProperties applicationProperties, MessageSource messageSource, SpringTemplateEngine templateEngine, MailgunClient mailgunClient) {
        this.applicationProperties = applicationProperties;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
        this.mailgunClient = mailgunClient;
    }

    public void sendMemberAssertionStatsMail(File stats) {
        LOGGER.debug("Sending member stats email to '{}'", applicationProperties.getMemberAssertionStatsRecipient());
        Context context = new Context(Locale.ENGLISH);
        String content = templateEngine.process("mail/memberAssertionStats", context);
        String subject = messageSource.getMessage("email.memberAssertionStats.title", null, Locale.ENGLISH);
        try {
            mailgunClient.sendMailWithAttachment(applicationProperties.getMemberAssertionStatsRecipient(), subject, content, stats);
        } catch (MailException e) {
            LOGGER.error("Error sending member stats email", e);
        }
    }

}
