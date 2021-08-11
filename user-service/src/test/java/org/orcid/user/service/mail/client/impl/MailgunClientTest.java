package org.orcid.user.service.mail.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.config.ApplicationProperties;
import org.orcid.user.service.mail.MailException;

public class MailgunClientTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private HttpClient httpClient;

    @Captor
    private ArgumentCaptor<HttpPost> postCaptor;

    @InjectMocks
    private MailgunClient mailgunClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(applicationProperties.getMailFromAddress()).thenReturn("mp@orcid.org");
        Mockito.when(applicationProperties.getMailFromName()).thenReturn("member portal");
        Mockito.when(applicationProperties.getMailApiUrl()).thenReturn("https://orcid.org");
    }

    @Test
    public void testSendMail() throws MailException, ClientProtocolException, IOException {
        Mockito.when(httpClient.execute(Mockito.any(HttpPost.class)))
                .thenReturn(new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK")));
        mailgunClient.sendMail("recipient@orcid.org", "test email", "<p>test email</p>");
        Mockito.verify(httpClient).execute(postCaptor.capture());
        HttpPost post = postCaptor.getValue();
        UrlEncodedFormEntity entity = (UrlEncodedFormEntity) post.getEntity();
        assertThat(entity.getContentType().getValue()).isEqualTo("application/x-www-form-urlencoded");

        List<NameValuePair> params = URLEncodedUtils.parse(post.getEntity());
        for (NameValuePair pair : params) {
            if (pair.getName().equals("to")) {
                assertThat(pair.getValue()).isEqualTo("recipient@orcid.org");
            }
            if (pair.getName().equals("subject")) {
                assertThat(pair.getValue()).isEqualTo("test email");
            }

            if (pair.getName().equals("html")) {
                assertThat(pair.getValue()).isEqualTo("<p>test email</p>");
            }
        }
    }

}
