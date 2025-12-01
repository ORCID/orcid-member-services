package org.orcid.mp.user.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MailgunClientTest {

    @Mock
    private RestClient httpClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec response;

    @Captor
    private ArgumentCaptor<Map<String, String>> formDataCaptor;

    private MailgunClient mailgunClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mailgunClient = new MailgunClient(httpClient);
        ReflectionTestUtils.setField(mailgunClient, "mailFromAddress", "memberportal@orcid.org");
        ReflectionTestUtils.setField(mailgunClient, "mailFromName", "Member Portal");
    }

    @Test
    public void testSendMail() throws IOException {
        when(httpClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(Mockito.any(Map.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(response);
        when(response.toEntity(String.class)).thenReturn(ResponseEntity.ok().build());

        mailgunClient.sendMail("recipient@orcid.org", "test email", "<p>test email</p>");

        verify(requestBodyUriSpec).body(formDataCaptor.capture());

        Map<String, String> params = formDataCaptor.getValue();
        assertThat(params.get("to")).isEqualTo("recipient@orcid.org");
        assertThat(params.get("subject")).isEqualTo("test email");
        assertThat(params.get("html")).isEqualTo("<p>test email</p>");
    }

}