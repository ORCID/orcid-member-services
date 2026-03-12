package org.orcid.mp.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.orcid.mp.member.MemberServiceApplication;
import org.orcid.mp.member.client.MailgunClient;
import org.orcid.mp.member.error.MailException;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MemberServiceApplication.class)
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
        mailService = new MailService(templateEngine, mailgunClient);
        ReflectionTestUtils.setField(mailService, "contactUpdateRecipient", "mp@orcid.org");
    }

    @Test
    void testSendAddContactEmail() throws MailException {
        MemberContactUpdate contactUpdate = new MemberContactUpdate();
        contactUpdate.setContactNewEmail("a.user@email.com");
        contactUpdate.setRequestedByEmail("requesting-user@email.com");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendAddContactEmail(contactUpdate);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
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
        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
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
        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
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
        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
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
        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.REMOVE_ORG_SUBJECT);
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
    }

}