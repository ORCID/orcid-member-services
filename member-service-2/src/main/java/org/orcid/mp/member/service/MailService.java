package org.orcid.mp.member.service;


import org.orcid.mp.member.client.MailgunClient;
import org.orcid.mp.member.error.MailException;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    static final String ADD_ORG_SUBJECT = "New organization request";

    static final String REMOVE_ORG_SUBJECT = "Remove member organization";

    @Value("${application.mail.contactUpdateRecipient}")
    private String contactUpdateRecipient;

    @Autowired
    private SpringTemplateEngine templateEngine;

    private MailgunClient mailgunClient;

    public MailService(SpringTemplateEngine templateEngine, MailgunClient mailgunClient) {
        this.templateEngine = templateEngine;
        this.mailgunClient = mailgunClient;
    }

    public void sendAddContactEmail(MemberContactUpdate memberContactUpdate) {
        LOGGER.debug("Sending add contact email to '{}'", contactUpdateRecipient);
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("requestedByName", memberContactUpdate.getRequestedByName());
        context.setVariable("requestedByEmail", memberContactUpdate.getRequestedByEmail());
        context.setVariable("requestedByMember", memberContactUpdate.getRequestedByMember());
        context.setVariable("currentMember", memberContactUpdate.getContactMember());
        context.setVariable("name", memberContactUpdate.getContactNewName());
        context.setVariable("email", memberContactUpdate.getContactNewEmail());
        context.setVariable("jobTitle", memberContactUpdate.getContactNewJobTitle());
        context.setVariable("phone", memberContactUpdate.getContactNewPhone());
        context.setVariable("roles", memberContactUpdate.getContactNewRoles());

        String content = templateEngine.process("mail/addContact", context);
        try {
            mailgunClient.sendMail(contactUpdateRecipient, memberContactUpdate.getRequestedByEmail(), CONTACT_UPDATE_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending add contact email to {}", contactUpdateRecipient, e);
        }
    }

    public void sendRemoveContactEmail(MemberContactUpdate memberContactUpdate) {
        LOGGER.debug("Sending remove contact email to '{}'", contactUpdateRecipient);
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("requestedByName", memberContactUpdate.getRequestedByName());
        context.setVariable("requestedByEmail", memberContactUpdate.getRequestedByEmail());
        context.setVariable("requestedByMember", memberContactUpdate.getRequestedByMember());
        context.setVariable("currentName", memberContactUpdate.getContactName());
        context.setVariable("currentEmail", memberContactUpdate.getContactEmail());
        context.setVariable("currentMember", memberContactUpdate.getContactMember());
        String content = templateEngine.process("mail/removeContact", context);
        try {
            mailgunClient.sendMail(contactUpdateRecipient, memberContactUpdate.getRequestedByEmail(), CONTACT_UPDATE_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending remove contact email to {}", contactUpdateRecipient, e);
        }
    }

    public void sendUpdateContactEmail(MemberContactUpdate memberContactUpdate) {
        LOGGER.debug("Sending update contact email to '{}'", contactUpdateRecipient);
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
            mailgunClient.sendMail(contactUpdateRecipient, memberContactUpdate.getRequestedByEmail(), CONTACT_UPDATE_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending update contact email to {}", contactUpdateRecipient, e);
        }
    }

    public void sendAddConsortiumMemberEmail(AddConsortiumMember addConsortiumMember) {
        LOGGER.debug("Sending add consortium member email to '{}'", contactUpdateRecipient);
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("requestedBy", addConsortiumMember.getRequestedByName() + " (" + addConsortiumMember.getRequestedByEmail() + ")");
        context.setVariable("consortium", addConsortiumMember.getConsortium());
        context.setVariable("orgName", addConsortiumMember.getOrgName());
        context.setVariable("consortiumMemberString", "Yes - " + addConsortiumMember.getConsortium());
        context.setVariable("emailDomain", addConsortiumMember.getEmailDomain());
        context.setVariable("street", addConsortiumMember.getStreet());
        context.setVariable("city", addConsortiumMember.getCity());
        context.setVariable("state", addConsortiumMember.getState());
        context.setVariable("country", addConsortiumMember.getCountry());
        context.setVariable("postcode", addConsortiumMember.getPostcode());
        context.setVariable("trademarkLicense", addConsortiumMember.getTrademarkLicense());
        context.setVariable("membershipStartDate", "01/" + addConsortiumMember.getStartMonth() + "/" + addConsortiumMember.getStartYear());
        context.setVariable("contactGivenName", addConsortiumMember.getContactGivenName());
        context.setVariable("contactFamilyName", addConsortiumMember.getContactFamilyName());
        context.setVariable("contactJobTitle", addConsortiumMember.getContactJobTitle());
        context.setVariable("contactEmail", addConsortiumMember.getContactEmail());
        context.setVariable("organizationTier", addConsortiumMember.getOrganizationTier());
        context.setVariable("integrationPlans", addConsortiumMember.getIntegrationPlans());

        String content = templateEngine.process("mail/addConsortiumMember", context);
        try {
            mailgunClient.sendMail(contactUpdateRecipient, addConsortiumMember.getRequestedByEmail(), ADD_ORG_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending add consortium member email to {}", contactUpdateRecipient, e);
        }
    }

    public void sendRemoveConsortiumMemberEmail(RemoveConsortiumMember removeConsortiumMember) {
        LOGGER.debug("Sending add consortium member email to '{}'", contactUpdateRecipient);
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("requestedBy", removeConsortiumMember.getRequestedByName() + " (" + removeConsortiumMember.getRequestedByEmail() + ")");
        context.setVariable("orgName", removeConsortiumMember.getOrgName());
        context.setVariable("consortium", removeConsortiumMember.getConsortium());
        context.setVariable("terminationDate", getTerminationDate(removeConsortiumMember.getTerminationMonth(), removeConsortiumMember.getTerminationYear()));

        String content = templateEngine.process("mail/removeConsortiumMember", context);
        try {
            mailgunClient.sendMail(contactUpdateRecipient, removeConsortiumMember.getRequestedByEmail(), REMOVE_ORG_SUBJECT, content);
        } catch (MailException e) {
            LOGGER.error("Error sending remove consortium member email to {}", contactUpdateRecipient, e);
        }
    }

    private String getTerminationDate(String terminationMonth, String terminationYear) {
        String date = "01/" + terminationMonth + "/" + terminationYear;
        LocalDate convertedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        convertedDate = convertedDate.withDayOfMonth(
            convertedDate.getMonth().length(convertedDate.isLeapYear()));
        String formattedDateString = convertedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return formattedDateString;
    }
}