package org.orcid.memberportal.service.user.services;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.client.ClientProtocolException;
import org.orcid.memberportal.service.user.config.ApplicationProperties;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.mail.MailException;
import org.orcid.memberportal.service.user.mail.client.impl.MailgunClient;
import org.orcid.memberportal.service.user.services.locale.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    private static final String MEMBER = "member";

    private static final String EMAIL = "email";

    private static final String INFO_EMAIL = "infoEmail";

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    private ApplicationProperties applicationProperties;

    private MailgunClient mailgunClient;

    public MailService(ApplicationProperties applicationProperties, MessageSource messageSource, SpringTemplateEngine templateEngine, MailgunClient mailgunClient) {
        this.applicationProperties = applicationProperties;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
        this.mailgunClient = mailgunClient;
    }

    public void sendActivationEmail(User user) {
        LOGGER.debug("Sending activation email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/activationEmail", "email.activation.title");
    }

    public void sendPasswordResetMail(User user) {
        LOGGER.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/passwordResetEmail", "email.reset.title");
    }

    public void sendOrganizationOwnerChangedMail(User user, String member) {
        LOGGER.debug("Sending organization owner changed email to '{}'", user.getEmail());
        sendEmailFromTemplateMemberInfo(user, member, "mail/organizationOwnerChanged", "email.organization.title");
    }

    private void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        LOGGER.debug("Preparing email using template {}", templateName);
        Locale locale = getLocale(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, applicationProperties.getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        try {
            sendEmail(user.getEmail(), subject, content);
        } catch (IOException e) {
            LOGGER.warn("Mail sending failure: {}", e.getMessage(), e);
        }
    }

    private void sendEmailFromTemplateMemberInfo(User user, String member, String templateName, String titleKey) {
        LOGGER.debug("Preparing email using template {}", templateName);
        Locale locale = getLocale(user.getLangKey());
        Context context = new Context(locale);
        String baseUrl = applicationProperties.getBaseUrl();
        String infoEmail = "info@member-portal.orcid.org";
        if ("https://member-portal.qa.orcid.org".equals(baseUrl)) {
            infoEmail = "info@member-portal.qa.orcid.org";
        } else if ("https://member-portal.sandbox.orcid.org".equals(baseUrl)) {
            infoEmail = "info@member-portal.sandbox.orcid.org";
        }

        context.setVariable(MEMBER, member + ".");
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, baseUrl);
        context.setVariable(INFO_EMAIL, infoEmail);
        context.setVariable(EMAIL, applicationProperties.getMailFromAddress());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);

        try {
            sendEmail(user.getEmail(), subject, content);
        } catch (IOException e) {
            LOGGER.warn("Mail sending failure: {}", e.getMessage(), e);
        }
    }

    private Locale getLocale(String langKey) {
        LOGGER.debug("Creating locale using language key {}", langKey);
        Locale locale = LocaleUtils.getLocale(langKey);
        LOGGER.debug("Locale created, locale has language {} ({})", new Object[] { locale.getLanguage(), locale.getDisplayLanguage() });
        return locale;
    }

    private void sendEmail(String to, String subject, String content) throws ClientProtocolException, IOException {
        try {
            mailgunClient.sendMail(to, subject, content);
        } catch (MailException e) {
            LOGGER.warn("Error sending email to {}", to, content, e);
        }
    }

}
