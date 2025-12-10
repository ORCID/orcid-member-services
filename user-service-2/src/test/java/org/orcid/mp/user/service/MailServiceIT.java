package org.orcid.mp.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.orcid.mp.user.UserServiceApplication;
import org.orcid.mp.user.client.MailgunClient;
import org.orcid.mp.user.config.Constants;
import org.orcid.mp.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.TestPropertySource;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = UserServiceApplication.class)
public class MailServiceIT {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Mock
    private MailgunClient mailgunClient;

    @Captor
    private ArgumentCaptor<String> subjectCaptor;

    @Captor
    private ArgumentCaptor<String> recipientCaptor;

    @Captor
    private ArgumentCaptor<String> contentCaptor;

    private MailService mailService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(mailgunClient).sendMail(anyString(), anyString(), anyString());
        mailService = new MailService(messageSource, templateEngine, mailgunClient);
    }

    @Test
    public void testSendActivationEmail() throws Exception {
        User user = new User();
        user.setLangKey(Constants.DEFAULT_LANGUAGE);
        user.setEmail("john.doe@example.com");
        mailService.sendActivationEmail(user);
        verify(mailgunClient, Mockito.times(1)).sendMail(recipientCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("john.doe@example.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("ORCID Member Portal activation");
        assertThat(contentCaptor.getValue()).isNotNull();
    }

    @Test
    public void testSendPasswordResetMail() throws Exception {
        User user = new User();
        user.setLangKey(Constants.DEFAULT_LANGUAGE);
        user.setEmail("john.doe@example.com");
        mailService.sendPasswordResetMail(user);
        verify(mailgunClient, Mockito.times(1)).sendMail(recipientCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("john.doe@example.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("ORCID Member Portal password reset");
        assertThat(contentCaptor.getValue()).isNotNull();
    }

    @Test
    public void testOrganizationOwnerChangedMail() throws Exception {
        User user = new User();
        user.setLangKey(Constants.DEFAULT_LANGUAGE);
        user.setEmail("john.doe@example.com");
        mailService.sendOrganizationOwnerChangedMail(user, "Member 1");
        verify(mailgunClient, Mockito.times(1)).sendMail(recipientCaptor.capture(), subjectCaptor.capture(), contentCaptor.capture());
        assertThat(recipientCaptor.getValue()).isEqualTo("john.doe@example.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("ORCID Member Portal organization owner updated");
        assertThat(contentCaptor.getValue()).isNotNull();

    }
}
