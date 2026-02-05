package org.orcid.mp.member.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.member.error.MailException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MailgunClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<MultiValueMap<String, String>> formDataCaptor;

    private MailgunClient mailgunClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Use openMocks instead of initMocks (deprecated)
        mailgunClient = new MailgunClient(restClient);
        ReflectionTestUtils.setField(mailgunClient, "mailFromAddress", "memberportal@orcid.org");
        ReflectionTestUtils.setField(mailgunClient, "mailFromName", "Member Portal");
    }

    @Test
    public void testSendMail() throws IOException, MailException {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("Success"));

        mailgunClient.sendMail("recipient@orcid.org", "test email", "<p>test email</p>");

        verify(requestBodySpec).body(formDataCaptor.capture());

        MultiValueMap<String, String> params = formDataCaptor.getValue();
        assertThat(params.getFirst("to")).isEqualTo("recipient@orcid.org");
        assertThat(params.getFirst("subject")).isEqualTo("test email");
        assertThat(params.getFirst("html")).isEqualTo("<p>test email</p>");
        assertThat(params.getFirst("from")).contains("memberportal@orcid.org");
    }
}