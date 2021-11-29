package org.orcid.memberportal.service.assertion.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.mail.MailException;
import org.orcid.memberportal.service.assertion.mail.client.impl.MailgunClient;
import org.orcid.memberportal.service.assertion.services.MailService;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring5.SpringTemplateEngine;

class MailServiceTest {

    @Mock
    private MessageSource messageSource;
    
    @Mock
    private SpringTemplateEngine templateEngine;
    
    @Mock
    private MailgunClient mailgunClient;

    @InjectMocks
    private MailService mailService;
    
    @Captor
    private ArgumentCaptor<String> recipientCaptor;
    
    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    
    @Captor
    private ArgumentCaptor<String> htmlCaptor;
    
    @Captor
    private ArgumentCaptor<File> fileCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(mailService, "applicationProperties", getTestApplicationProperties());
        Mockito.when(messageSource.getMessage(Mockito.eq("email.memberAssertionStats.title"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("member stats");
        Mockito.when(templateEngine.process(Mockito.eq("mail/memberAssertionStats"), Mockito.any(IContext.class))).thenReturn("something");
    }

    @Test
    void testSendMemberAssertionStatsMail() throws MailException {
        Mockito.doNothing().when(mailgunClient).sendMailWithAttachment(Mockito.eq("memberstats@orcid.org"), Mockito.eq("member stats"), Mockito.eq("something"),
                Mockito.any(File.class));
        mailService.sendMemberAssertionStatsMail(getAttachment());
        Mockito.verify(mailgunClient).sendMailWithAttachment(recipientCaptor.capture(), subjectCaptor.capture(), htmlCaptor.capture(), fileCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("memberstats@orcid.org");
        assertThat(subjectCaptor.getValue()).isEqualTo("member stats");
        assertThat(htmlCaptor.getValue()).isEqualTo("something");
        assertThat(fileCaptor.getValue()).isNotNull();
    }

    private ApplicationProperties getTestApplicationProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setMemberAssertionStatsRecipient("memberstats@orcid.org");
        return properties;
    }
    
    private File getAttachment() {
        return new File(getClass().getResource("/assertions-with-bad-email.csv").getFile());
    }

}
