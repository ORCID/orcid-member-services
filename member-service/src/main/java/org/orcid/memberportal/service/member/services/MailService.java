package org.orcid.memberportal.service.member.services;

import org.orcid.memberportal.service.member.config.ApplicationProperties;
import org.orcid.memberportal.service.member.mail.MailException;
import org.orcid.memberportal.service.member.mail.client.impl.MailgunClient;
import org.orcid.memberportal.service.member.web.rest.vm.MemberContactUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.Locale;

/**
 * Service for sending emails.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
@Async
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    static final String CONTACT_UPDATE_SUBJECT = "Organization contact change";

    @Autowired
    private SpringTemplateEngine templateEngine;

    private ApplicationProperties applicationProperties;

    private MailgunClient mailgunClient;

    public MailService(ApplicationProperties applicationProperties, SpringTemplateEngine templateEngine, MailgunClient mailgunClient) {
        this.applicationProperties = applicationProperties;
        this.templateEngine = templateEngine;
        this.mailgunClient = mailgunClient;
    }

    public void sendAddContactEmail(MemberContactUpdate memberContactUpdate) {
        LOGGER.debug("Sending add contact email to '{}'", applicationProperties.getContactUpdateRecipient());
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("requestedByName", memberContactUpdate.getRequestedByName());
        context.setVariable("requestedByEmail", memberContactUpdate.getRequestedByEmail());
        context.setVariable("requestedByMember", memberContactUpdate.getRequestedByMember());
        context.setVariable("name", memberContactUpdate.getContactNewName());
        context.setVariable("email", memberContactUpdate.getContactNewEmail());
        context.setVariable("jobTitle", memberContactUpdate.getContactNewJobTitle());
        context.setVariable("phone", memberContactUpdate.getContactNewPhone());
        context.setVariable("roles", memberContactUpdate.getContactNewRoles());

        String content = templateEngine.process("mail/addContact", context);
        try {
            mailgunClient.sendMail(applicationProperties.getContactUpdateRecipient(), memberContactUpdate.getRequestedByEmail(), CONTACT_UPDATE_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending add contact email to {}", applicationProperties.getContactUpdateRecipient(), e);
        }
    }

    public void sendRemoveContactEmail(MemberContactUpdate memberContactUpdate) {
        LOGGER.debug("Sending remove contact email to '{}'", applicationProperties.getContactUpdateRecipient());
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("requestedByName", memberContactUpdate.getRequestedByName());
        context.setVariable("requestedByEmail", memberContactUpdate.getRequestedByEmail());
        context.setVariable("requestedByMember", memberContactUpdate.getRequestedByMember());
        context.setVariable("currentName", memberContactUpdate.getContactName());
        context.setVariable("currentEmail", memberContactUpdate.getContactEmail());
        context.setVariable("currentMember", memberContactUpdate.getContactMember());
        String content = templateEngine.process("mail/removeContact", context);
        try {
            mailgunClient.sendMail(applicationProperties.getContactUpdateRecipient(), memberContactUpdate.getRequestedByEmail(), CONTACT_UPDATE_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending remove contact email to {}", applicationProperties.getContactUpdateRecipient(), e);
        }
    }

    public void sendUpdateContactEmail(MemberContactUpdate memberContactUpdate) {
        LOGGER.debug("Sending update contact email to '{}'", applicationProperties.getContactUpdateRecipient());
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("requestedByName", memberContactUpdate.getRequestedByName());
        context.setVariable("requestedByEmail", memberContactUpdate.getRequestedByEmail());
        context.setVariable("requestedByMember", memberContactUpdate.getRequestedByMember());
        context.setVariable("currentName", memberContactUpdate.getContactName());
        context.setVariable("currentEmail", memberContactUpdate.getContactEmail());
        context.setVariable("currentMember", memberContactUpdate.getContactMember());
        context.setVariable("name", memberContactUpdate.getContactNewName());
        context.setVariable("email", memberContactUpdate.getContactNewEmail());
        context.setVariable("jobTitle", memberContactUpdate.getContactNewJobTitle());
        context.setVariable("phone", memberContactUpdate.getContactNewPhone());
        context.setVariable("roles", memberContactUpdate.getContactNewRoles());
        String content = templateEngine.process("mail/updateContact", context);
        try {
            mailgunClient.sendMail(applicationProperties.getContactUpdateRecipient(), memberContactUpdate.getRequestedByEmail(), CONTACT_UPDATE_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending update contact email to {}", applicationProperties.getContactUpdateRecipient(), e);
        }
    }

}
