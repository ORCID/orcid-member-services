package org.orcid.memberportal.service.member.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.member.MemberServiceApp;
import org.orcid.memberportal.service.member.config.ApplicationProperties;
import org.orcid.memberportal.service.member.mail.MailException;
import org.orcid.memberportal.service.member.mail.client.impl.MailgunClient;
import org.orcid.memberportal.service.member.web.rest.vm.AddConsortiumMember;
import org.orcid.memberportal.service.member.web.rest.vm.MemberContactUpdate;
import org.orcid.memberportal.service.member.web.rest.vm.RemoveConsortiumMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MemberServiceApp.class)
class MailServiceIT {

    @Mock
    private MailgunClient mailgunClient;

    @Autowired
    private SpringTemplateEngine templateEngine;

    private MailService mailService;

    @Captor
    private ArgumentCaptor<String> recipientCaptor;

    @Captor
    private ArgumentCaptor<String> subjectCaptor;

    @Captor
    private ArgumentCaptor<String> ccCaptor;

    @Captor
    private ArgumentCaptor<File> fileCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mailService = new MailService(getTestApplicationProperties(), templateEngine, mailgunClient);
    }

    @Test
    void testSendAddContactEmail() throws MailException {
        MemberContactUpdate contactUpdate = new MemberContactUpdate();
        contactUpdate.setContactNewEmail("a.user@email.com");
        contactUpdate.setRequestedByEmail("requesting-user@email.com");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendAddContactEmail(contactUpdate);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("contactUpdate@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.CONTACT_UPDATE_SUBJECT);
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
    }

    @Test
    void testSendRemoveContactEmail() throws MailException {
        MemberContactUpdate contactUpdate = new MemberContactUpdate();
        contactUpdate.setContactEmail("a.user@email.com");
        contactUpdate.setRequestedByEmail("requesting-user@email.com");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendRemoveContactEmail(contactUpdate);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("contactUpdate@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.CONTACT_UPDATE_SUBJECT);
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
    }

    @Test
    void testSendUpdateContactEmail() throws MailException {
        MemberContactUpdate contactUpdate = new MemberContactUpdate();
        contactUpdate.setContactEmail("a.user@email.com");
        contactUpdate.setContactNewEmail("a.new.user@email.com");
        contactUpdate.setRequestedByEmail("requesting-user@email.com");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendUpdateContactEmail(contactUpdate);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("contactUpdate@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.CONTACT_UPDATE_SUBJECT);
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
    }

    @Test
    void testSendAddConsortiumMemberEmail() throws MailException {
        AddConsortiumMember addConsortiumMember = new AddConsortiumMember();
        addConsortiumMember.setRequestedByEmail("requesting-user@email.com");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendAddConsortiumMemberEmail(addConsortiumMember);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("contactUpdate@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.ADD_ORG_SUBJECT);
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
    }

    @Test
    void testSendRemoveConsortiumMemberEmail() throws MailException {
        RemoveConsortiumMember removeConsortiumMember = new RemoveConsortiumMember();
        removeConsortiumMember.setRequestedByEmail("requesting-user@email.com");
        removeConsortiumMember.setTerminationMonth("12");
        removeConsortiumMember.setTerminationYear("2024");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendRemoveConsortiumMemberEmail(removeConsortiumMember);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("contactUpdate@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.REMOVE_ORG_SUBJECT);
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
    }

    private ApplicationProperties getTestApplicationProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setContactUpdateRecipient("contactUpdate@orcid.org");
        properties.setMailTestMode(true);
        return properties;
    }

}
