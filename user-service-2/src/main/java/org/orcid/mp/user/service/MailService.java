package org.orcid.mp.user.service;

import java.util.Locale;

import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.client.MailgunClient;
import org.orcid.mp.user.util.LocaleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;


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

    @Value("${application.baseUrl}")
    private String baseUrl;

    @Value("${application.mail.fromAddress}")
    private String mailFromAddress;

    private MailgunClient mailgunClient;

    public MailService(MessageSource messageSource, SpringTemplateEngine templateEngine, MailgunClient mailgunClient) {
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
        context.setVariable(BASE_URL, baseUrl);
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmail(user.getEmail(), subject, content);
    }

    private void sendEmailFromTemplateMemberInfo(User user, String member, String templateName, String titleKey) {
        LOGGER.debug("Preparing email using template {}", templateName);
        Locale locale = getLocale(user.getLangKey());
        Context context = new Context(locale);
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
        context.setVariable(EMAIL, mailFromAddress);
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmail(user.getEmail(), subject, content);
    }

    private Locale getLocale(String langKey) {
        LOGGER.debug("Creating locale using language key {}", langKey);
        Locale locale = LocaleUtil.getLocale(langKey);
        LOGGER.debug("Locale created, locale has language {} ({})", new Object[]{locale.getLanguage(), locale.getDisplayLanguage()});
        return locale;
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            mailgunClient.sendMail(to, subject, content);
        } catch (Exception e) {
            LOGGER.warn("Error sending email to {}", to, content, e);
        }
    }

}
