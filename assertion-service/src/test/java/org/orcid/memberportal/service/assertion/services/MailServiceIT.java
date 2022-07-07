package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.AssertionServiceApp;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.mail.MailException;
import org.orcid.memberportal.service.assertion.mail.client.impl.MailgunClient;
import org.orcid.memberportal.service.assertion.upload.AssertionsUploadSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.thymeleaf.spring5.SpringTemplateEngine;

@SpringBootTest(classes = AssertionServiceApp.class)
class MailServiceIT {

    @Mock
    private MessageSource messageSource;
    
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
    private ArgumentCaptor<File> fileCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mailService = new MailService(getTestApplicationProperties(), messageSource, templateEngine, mailgunClient);
        Mockito.when(messageSource.getMessage(Mockito.eq("email.memberAssertionStats.title"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("member stats");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.affiliationUploadSummary.title"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("summary");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.notificationsSummary.title"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("notifications summary");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.invitation.title"), Mockito.any(), Mockito.any(Locale.class))).thenReturn("someone wants to add something to your record");
    }
    
    @Test
    void testSendCsvReportMail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMailWithAttachment(Mockito.eq("memberstats@orcid.org"), Mockito.eq("member stats"), Mockito.eq("something"),
                Mockito.any(File.class));
        mailService.sendCsvReportMail(getAttachment(), getUser(), "subject", "content");
        
        Mockito.verify(mailgunClient).sendMailWithAttachment(recipientCaptor.capture(), subjectCaptor.capture(), Mockito.anyString(), fileCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("summary@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo("subject");
        assertThat(fileCaptor.getValue()).isNotNull();
    }

    @Test
    void testSendMemberAssertionStatsMail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMailWithAttachment(Mockito.eq("memberstats@orcid.org"), Mockito.eq("member stats"), Mockito.eq("something"),
                Mockito.any(File.class));
        mailService.sendMemberAssertionStatsMail(getAttachment());
        Mockito.verify(mailgunClient).sendMailWithAttachment(recipientCaptor.capture(), subjectCaptor.capture(), Mockito.anyString(), fileCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("memberstats@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo("member stats");
        assertThat(fileCaptor.getValue()).isNotNull();
    }
    
    @Test
    void testSendAssertionsUploadSummaryMail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.eq("summary@orcid.org"), Mockito.eq("summary"), Mockito.eq("something"));
        mailService.sendAssertionsUploadSummaryMail(getUploadSummary(), getUser());
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("summary@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo("summary");
    }
    
    @Test
    void testSendNotificationsSummaryMail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.eq("summary@orcid.org"), Mockito.eq("summary"), Mockito.eq("something"));
        mailService.sendNotificationsSummary(getUser(), 10, 5);
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("summary@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo("notifications summary");
    }
    
    @Test
    void testSendInvitationEmail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMail(Mockito.eq("summary@orcid.org"), Mockito.eq("summary"), Mockito.eq("something"));
        mailService.sendInvitationEmail("summary@orcid.org", "some org", "some/base/address?state=some-state-value");
        Mockito.verify(mailgunClient).sendMail(recipientCaptor.capture(), subjectCaptor.capture(), Mockito.anyString());
        assertThat(recipientCaptor.getValue()).isEqualTo("summary@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo("someone wants to add something to your record");
    }

    private AssertionServiceUser getUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setLangKey("en");
        user.setEmail("summary@orcid.org");
        return user;
    }

    private AssertionsUploadSummary getUploadSummary() {
        AssertionsUploadSummary summary = new AssertionsUploadSummary();
        summary.setNumAdded(1);
        summary.setNumDeleted(1);
        summary.setNumDuplicates(1);
        summary.setNumUpdated(1);
        return summary;
    }

    private ApplicationProperties getTestApplicationProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setMemberAssertionStatsRecipient("memberstats@orcid.org");
        properties.setMailTestMode(true);
        return properties;
    }
    
    private File getAttachment() {
        return new File(getClass().getResource("/assertions-with-bad-email.csv").getFile());
    }

}
