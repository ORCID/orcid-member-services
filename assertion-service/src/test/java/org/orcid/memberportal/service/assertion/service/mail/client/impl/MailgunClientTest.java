package org.orcid.memberportal.service.assertion.service.mail.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
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
import org.orcid.memberportal.service.assertion.mail.MailException;
import org.orcid.memberportal.service.assertion.mail.client.impl.MailgunClient;

class MailgunClientTest {

    @InjectMocks
    private MailgunClient mailgunClient;

    @Mock
    private HttpClient client;

    @Captor
    private ArgumentCaptor<HttpPost> postCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mailgunClient.setFromName("test");
        mailgunClient.setFromAddress("test@orcid.org");
        mailgunClient.setTestMode(false);
        mailgunClient.setMailApiUrl("https://some/api/url");
    }

    @Test
    void testSendMailWithAttachment() throws MailException, ClientProtocolException, IOException {
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(getTestHttpResponse());

        mailgunClient.sendMailWithAttachment("user@orcid.org", "test email with attachment", "<p>some html</p>", getAttachment());

        Mockito.verify(client).execute(postCaptor.capture());
        HttpPost capturedPost = postCaptor.getValue();
        HttpEntity capturedEntity = capturedPost.getEntity();
        assertThat(capturedEntity.getContentType().getName()).isEqualTo("Content-Type");
        assertThat(capturedEntity.getContentType().getValue()).startsWith("multipart/form-data");

        String data = new String(capturedEntity.getContent().readAllBytes(), "utf-8");
        assertThat(data).contains("name=\"to\"");
        assertThat(data).contains("user@orcid.org");
        assertThat(data).contains("name=\"from\"");
        assertThat(data).contains("test <test@orcid.org>");
        assertThat(data).contains("name=\"subject\"");
        assertThat(data).contains("test email with attachment");
        assertThat(data).contains("name=\"html\"");
        assertThat(data).contains("<p>some html</p>");
        assertThat(data).contains("name=\"attachment\"");
        assertThat(data).contains("filename=\"assertions-with-bad-email.csv\"");

        // check first line of attached file present in email
        assertThat(data).contains(
                "email,affiliation-section,department-name,role-title,url,start-date,end-date,org-name,org-country,org-city,org-region,disambiguated-organization-identifier,disambiguation-source");
    }
    
    @Test
    void testSendMail() throws MailException, ClientProtocolException, IOException {
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(getTestHttpResponse());

        mailgunClient.sendMail("user@orcid.org", "test email with attachment", "<p>some html</p>");

        Mockito.verify(client).execute(postCaptor.capture());
        HttpPost capturedPost = postCaptor.getValue();
        HttpEntity capturedEntity = capturedPost.getEntity();
        assertThat(capturedEntity.getContentType().getName()).isEqualTo("Content-Type");
        assertThat(capturedEntity.getContentType().getValue()).startsWith("multipart/form-data");

        String data = new String(capturedEntity.getContent().readAllBytes(), "utf-8");
        assertThat(data).contains("name=\"to\"");
        assertThat(data).contains("user@orcid.org");
        assertThat(data).contains("name=\"from\"");
        assertThat(data).contains("test <test@orcid.org>");
        assertThat(data).contains("name=\"subject\"");
        assertThat(data).contains("test email with attachment");
        assertThat(data).contains("name=\"html\"");
        assertThat(data).contains("<p>some html</p>");
    }

    private HttpResponse getTestHttpResponse() {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("https", 1, 2), 200, "OK"));
        return response;
    }

    private File getAttachment() {
        return new File(getClass().getResource("/assertions-with-bad-email.csv").getFile());
    }

}
