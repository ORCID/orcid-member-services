package org.orcid.mp.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.orcid.mp.member.MemberServiceApplication;
import org.orcid.mp.member.apicreds.AMCredentialsApplication;
import org.orcid.mp.member.apicreds.ApiClientUpdateNotification;
import org.orcid.mp.member.apicreds.ProductionCredentialsApplication;
import org.orcid.mp.member.apicreds.SlClientChangeRequest;
import org.orcid.mp.member.client.MailgunClient;
import org.orcid.mp.member.error.MailException;
import org.orcid.mp.member.salesforce.request.AddConsortiumMember;
import org.orcid.mp.member.salesforce.request.MemberContactUpdate;
import org.orcid.mp.member.salesforce.request.RemoveConsortiumMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.util.List;

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
    private ArgumentCaptor<String> contentCaptor;

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

    @Test
    void testSendApplyForProdCredsEmail() throws MailException {
        ProductionCredentialsApplication application = new ProductionCredentialsApplication();
        application.setRequestedByEmail("requesting-user@email.com");
        application.setOrgName("Test Org");
        application.setIntegrationDescription("description");
        application.setNotes("notes");
        application.setSystemIntegrationType("something here");
        application.setAuthenticateIdsAnswer("yes - some details here");
        application.setRequestedByName("someone");
        application.setIntegrationHomepageUrl("https://some.url");
        application.setRedirectUris(List.of("https://some.url", "https://some-other.url"));

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendApplyForProdCredsEmail(application);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.PROD_CREDS_SUBJECT_PREFIX + "Test Org");
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
        assertThat(contentCaptor.getValue()).contains("description");
        assertThat(contentCaptor.getValue()).contains("something here");
        assertThat(contentCaptor.getValue()).contains("yes - some details here");
        assertThat(contentCaptor.getValue()).contains("notes");
        assertThat(contentCaptor.getValue()).contains("https://some.url");
        assertThat(contentCaptor.getValue()).contains("https://some-other.url");
        assertThat(contentCaptor.getValue()).contains("someone (requesting-user@email.com)");
        assertThat(contentCaptor.getValue()).contains("Test Org");
    }

    @Test
    void testSendApplyForProdCredsEmail_withCLEmail() throws MailException {
        ProductionCredentialsApplication application = new ProductionCredentialsApplication();
        application.setRequestedByEmail("requesting-user@email.com");
        application.setOrgName("Test Org");
        application.setIntegrationDescription("description");
        application.setNotes("notes");
        application.setSystemIntegrationType("something here");
        application.setAuthenticateIdsAnswer("yes - some details here");
        application.setRequestedByName("someone");
        application.setIntegrationHomepageUrl("https://some.url");
        application.setRedirectUris(List.of("https://some.url", "https://some-other.url"));
        application.setConsortiumLeadEmail("cl@orcid.org");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        mailService.sendApplyForProdCredsEmail(application);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org, cl@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.PROD_CREDS_SUBJECT_PREFIX + "Test Org");
        assertThat(ccCaptor.getValue()).isEqualTo("requesting-user@email.com");
        assertThat(contentCaptor.getValue()).contains("description");
        assertThat(contentCaptor.getValue()).contains("something here");
        assertThat(contentCaptor.getValue()).contains("yes - some details here");
        assertThat(contentCaptor.getValue()).contains("notes");
        assertThat(contentCaptor.getValue()).contains("https://some.url");
        assertThat(contentCaptor.getValue()).contains("https://some-other.url");
        assertThat(contentCaptor.getValue()).contains("someone (requesting-user@email.com)");
        assertThat(contentCaptor.getValue()).contains("Test Org");
    }

    @Test
    void testSendClientUpdatedEmail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        ApiClientUpdateNotification notification = clientUpdate();
        notification.setHomepageUrl("https://updated-homepage-url.org");
        notification.setAddedRedirectUris(List.of("https://added.example.org/cb"));
        notification.setRemovedRedirectUris(List.of("https://removed.example.org/cb"));
        mailService.sendClientUpdatedEmail(notification);

        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("main@member.org, tech@member.org, owner@member.org");
        assertThat(ccCaptor.getValue()).isEqualTo("mp@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo("Update to your ORCID Member API Credentials");

        String content = contentCaptor.getValue();
        assertThat(content).contains("A change has been made to your Member API Credentials for the client below:");
        assertThat(content).contains("My client");
        assertThat(content).contains("APP-1234567890ABCDEF");
        assertThat(content).contains("What has changed?");
        assertThat(content).contains("Homepage URL");
        assertThat(content).contains("https://updated-homepage-url.org");
        assertThat(content).contains("Redirect URIs added");
        assertThat(content).contains("https://added.example.org/cb");
        assertThat(content).contains("Redirect URIs removed");
        assertThat(content).contains("https://removed.example.org/cb");
        assertThat(content).doesNotContain("<strong>Description</strong>");
        assertThat(content).doesNotContain("has been reset");
        assertThat(content).contains("Member Support Team");
        assertThat(content).contains("mailto:mp@orcid.org");
    }

    @Test
    void testSendClientSecretResetEmail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        ApiClientUpdateNotification notification = clientUpdate();
        notification.setSecretReset(true);
        mailService.sendClientUpdatedEmail(notification);

        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());
        String content = contentCaptor.getValue();
        assertThat(content).contains("Client secret");
        assertThat(content).contains("The client secret has been reset");
        assertThat(content).doesNotContain("<strong>Homepage URL</strong>");
        assertThat(content).doesNotContain("Redirect URIs added");
    }

    private ApiClientUpdateNotification clientUpdate() {
        ApiClientUpdateNotification notification = new ApiClientUpdateNotification();
        notification.setRecipients(List.of("main@member.org", "tech@member.org", "owner@member.org"));
        notification.setClientId("APP-1234567890ABCDEF");
        notification.setClientName("My client");
        return notification;
    }

    @Test
    void testRequestSLClientChangeEmail() throws MailException {
        SlClientChangeRequest request = new SlClientChangeRequest();
        request.setRequestedByName("Andrew User");
        request.setRequestedByEmail("a.user@aaf.edu.au");
        request.setOrgName("Australian Access Federation - AAF");
        request.setHomepageUrl("http://updated-homepage-url.com");
        request.setRedirectUris(List.of("https://redirect1.url", "https://redirect2.url"));

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        mailService.requestSLClientChange(request);

        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());

        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.SL_CLIENT_CHANGE_SUBJECT);
        assertThat(ccCaptor.getValue()).isEqualTo("a.user@aaf.edu.au");

        String content = contentCaptor.getValue();
        assertThat(content).contains("Andrew User (a.user@aaf.edu.au)");
        assertThat(content).contains("Australian Access Federation - AAF");
        assertThat(content).contains("http://updated-homepage-url.com");
        assertThat(content).contains("https://redirect1.url");
        assertThat(content).contains("https://redirect2.url");
    }

    @Test
    void testApplyForAMCredsEmail() throws MailException {
        AMCredentialsApplication application = new AMCredentialsApplication();
        application.setRequestedByName("Sarah Smith");
        application.setRequestedByEmail("sarah.smith@example.com");
        application.setOrgName("Test AM Org");
        application.setConsortiumLeadName("Lead Name");
        application.setOrgHomePage("https://am-org.com");
        application.setNotes("Please approve quickly.");

        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        mailService.applyForAMCreds(application);

        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), ccCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());

        assertThat(recipientCaptor.getValue()).isEqualTo("mp@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo(MailService.AM_CREDENTIALS_APPLICATION);
        assertThat(ccCaptor.getValue()).isEqualTo("sarah.smith@example.com");

        String content = contentCaptor.getValue();
        assertThat(content).contains("Sarah Smith (sarah.smith@example.com)");
        assertThat(content).contains("Test AM Org");
        assertThat(content).contains("Lead Name");
        assertThat(content).contains("https://am-org.com");
        assertThat(content).contains("Please approve quickly.");
    }

}