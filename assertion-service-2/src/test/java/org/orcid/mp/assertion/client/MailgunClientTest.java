package org.orcid.mp.assertion.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.orcid.mp.assertion.error.MailException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MailgunClientTest {

    private MailgunClient mailgunClient;

    @Mock
    private RestClient restClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<MultiValueMap<String, Object>> formDataCaptor;

    @BeforeEach
    public void setUp() {
        mailgunClient = new MailgunClient(restClient);

        ReflectionTestUtils.setField(mailgunClient, "mailFromName", "test");
        ReflectionTestUtils.setField(mailgunClient, "mailFromAddress", "test@orcid.org");
        ReflectionTestUtils.setField(mailgunClient, "testMode", false);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.toEntity(String.class)).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
    }

    @Test
    void testSendMail() throws MailException {
        mailgunClient.sendMail("user@orcid.org", "test email", "<p>some html</p>");

        verify(requestBodyUriSpec).contentType(MediaType.APPLICATION_FORM_URLENCODED);

        verify(requestBodyUriSpec).body(formDataCaptor.capture());
        MultiValueMap<String, Object> params = formDataCaptor.getValue();

        assertThat(params.getFirst("to")).isEqualTo("user@orcid.org");
        assertThat(params.getFirst("subject")).isEqualTo("test email");
        assertThat(params.getFirst("html")).isEqualTo("<p>some html</p>");
        assertThat(params.getFirst("from")).isEqualTo("test <test@orcid.org>");

        assertThat(params.containsKey("o:testmode")).isFalse();
    }

    @Test
    void testSendMailWithAttachment() throws MailException, IOException {
        mailgunClient.sendMailWithAttachment("user@orcid.org", "test email with attachment", "<p>some html</p>", getAttachment());

        verify(requestBodyUriSpec).contentType(MediaType.MULTIPART_FORM_DATA);

        verify(requestBodyUriSpec).body(formDataCaptor.capture());
        MultiValueMap<String, Object> params = formDataCaptor.getValue();

        assertThat(params.getFirst("to")).isEqualTo("user@orcid.org");
        assertThat(params.getFirst("subject")).isEqualTo("test email with attachment");
        assertThat(params.getFirst("html")).isEqualTo("<p>some html</p>");
        assertThat(params.getFirst("from")).isEqualTo("test <test@orcid.org>");

        Object attachment = params.getFirst("attachment");
        assertThat(attachment).isInstanceOf(FileSystemResource.class);
        FileSystemResource resource = (FileSystemResource) attachment;
        assertThat(resource.getFilename()).startsWith("assertions-with-bad-email");
    }

    @Test
    void testSendMailInTestMode() throws MailException {
        ReflectionTestUtils.setField(mailgunClient, "testMode", true);

        mailgunClient.sendMail("user@orcid.org", "test email", "html");

        verify(requestBodyUriSpec).body(formDataCaptor.capture());
        MultiValueMap<String, Object> params = formDataCaptor.getValue();

        assertThat(params.getFirst("o:testmode")).isEqualTo("yes");
    }

    private File getAttachment() throws IOException {
        File tempFile = File.createTempFile("assertions-with-bad-email", ".csv");
        tempFile.deleteOnExit();
        return tempFile;
    }
}