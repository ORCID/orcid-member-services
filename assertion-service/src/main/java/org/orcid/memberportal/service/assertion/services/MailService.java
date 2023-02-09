package org.orcid.memberportal.service.assertion.services;

import java.io.File;
import java.util.Locale;

import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.mail.MailException;
import org.orcid.memberportal.service.assertion.mail.client.impl.MailgunClient;
import org.orcid.memberportal.service.assertion.services.locale.LocaleUtils;
import org.orcid.memberportal.service.assertion.upload.AssertionsUploadSummary;
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

    public void sendCsvReportMail(File report, AssertionServiceUser user, String subject, String text) {
        LOGGER.debug("Sending csv report email to '{}'", user.getEmail());
        Locale locale = LocaleUtils.getLocale(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable("text", text);
        context.setVariable("subject", subject);
        String content = templateEngine.process("mail/csvReport", context);
        try {
            mailgunClient.sendMailWithAttachment(user.getEmail(), subject, content, report);
        } catch (MailException e) {
            LOGGER.error("Error sending csv report email to {}", user.getEmail(), e);
        }
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

    public void sendAssertionsUploadSummaryMail(AssertionsUploadSummary summary, AssertionServiceUser user) {
        Locale locale = LocaleUtils.getLocale(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable("summary", summary);
        String content = templateEngine.process("mail/affiliationUploadSummary", context);
        String subject = messageSource.getMessage("email.affiliationUploadSummary.title", null, locale);
        try {
            mailgunClient.sendMail(user.getEmail(), subject, content);
        } catch (MailException e) {
            LOGGER.error("Error sending csv upload summary email to {}", user.getEmail(), e);
        }
    }

    public void sendNotificationsSummary(AssertionServiceUser user, Integer notificationsSent, Integer emailsSent) {
        Locale locale = LocaleUtils.getLocale(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable("notificationsSent", notificationsSent);
        context.setVariable("emailsSent", emailsSent);
        String content = templateEngine.process("mail/notificationsSummary", context);
        String subject = messageSource.getMessage("email.notificationsSummary.title", null, locale);
        try {
            mailgunClient.sendMail(user.getEmail(), subject, content);
        } catch (MailException e) {
            LOGGER.error("Error sending notifications summary email to {}", user.getEmail(), e);
        }
    }

    public void sendInvitationEmail(String email, String orgName, String permissionLink, String language) {
        Locale locale = LocaleUtils.getLocale(language);
        Context context = new Context(locale);
        context.setVariable("orgName", orgName);
        context.setVariable("permissionLink", permissionLink);

        String content = templateEngine.process("mail/invitation", context);
        String subject = messageSource.getMessage("email.invitation.title", new Object[] { orgName }, locale);
        try {
            mailgunClient.sendMail(email, subject, content);
        } catch (MailException e) {
            LOGGER.error("Error sending invitation email to {}", email, e);
        }
    }

}
