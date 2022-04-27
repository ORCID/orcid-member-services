package org.orcid.memberportal.service.assertion.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.jaxb.model.v3.release.error.OrcidError;
import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.jaxb.model.v3.release.record.Distinction;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.InvitedPosition;
import org.orcid.jaxb.model.v3.release.record.Membership;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.Service;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.Notification;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;

public class OrcidAPIClientTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private CloseableHttpClient httpClient;

    @InjectMocks
    private OrcidAPIClient client;

    @Captor
    private ArgumentCaptor<HttpUriRequest> requestCaptor;

    private JAXBContext jaxbContext;

    private Unmarshaller unmarshaller;

    @BeforeEach
    public void setUp() throws JAXBException {
        MockitoAnnotations.initMocks(this);
        jaxbContext = JAXBContext.newInstance(Affiliation.class, Distinction.class, Employment.class, Education.class, InvitedPosition.class, Membership.class,
                Qualification.class, Service.class, OrcidError.class, NotificationPermission.class);
        unmarshaller = jaxbContext.createUnmarshaller();
    }

    @Test
    void testPostNotification() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/v3/");
        Mockito.when(applicationProperties.getNotificationAccessToken()).thenReturn("access-token");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenReturn(new OrcidCloseableHttpResponse());

        Notification notification = getNotification();
        String putCode = client.postNotification(notification);

        assertThat(putCode).isEqualTo("put-code");

        Mockito.verify(applicationProperties).getOrcidAPIEndpoint();
        Mockito.verify(applicationProperties).getNotificationAccessToken();
        Mockito.verify(httpClient).execute(requestCaptor.capture());

        HttpUriRequest request = requestCaptor.getValue();
        assertThat(request.getURI().toString()).isEqualTo("orcid/v3/orcid/notification-permission");

        HttpEntityEnclosingRequest requestWithBody = (HttpEntityEnclosingRequest) request;
        HttpEntity entity = requestWithBody.getEntity();

        assertThat(entity.getContentType().getName()).isEqualTo("Content-Type");
        assertThat(entity.getContentType().getValue()).startsWith("text/xml");
        
        NotificationPermission notificationPermission = (NotificationPermission) unmarshaller.unmarshal(entity.getContent());
        assertThat(notificationPermission.getNotificationSubject()).isEqualTo("subject");
        assertThat(notificationPermission.getNotificationType()).isEqualTo(NotificationType.PERMISSION);
        assertThat(notificationPermission.getNotificationIntro()).isEqualTo("intro");
        assertThat(notificationPermission.getItems()).isNotNull();
        assertThat(notificationPermission.getItems().getItems()).isNotNull();
        assertThat(notificationPermission.getItems().getItems().size()).isEqualTo(1);
        assertThat(notificationPermission.getItems().getItems().get(0).getItemName()).isEqualTo("name");
        assertThat(notificationPermission.getItems().getItems().get(0).getItemType()).isEqualTo(ItemType.DISTINCTION);
    }

    private Notification getNotification() {
        Notification notification = new Notification();
        notification.setIntro("intro");
        notification.setName("name");
        notification.setOrcidId("orcid");
        notification.setSubject("subject");
        notification.setType(AffiliationSection.DISTINCTION);
        return notification;
    }

    private class OrcidCloseableHttpResponse implements CloseableHttpResponse {

        @Override
        public StatusLine getStatusLine() {
            return new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 201, "CREATED");
        }

        @Override
        public void setStatusLine(StatusLine statusline) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code, String reason) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setStatusCode(int code) throws IllegalStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setReasonPhrase(String reason) throws IllegalStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public HttpEntity getEntity() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setEntity(HttpEntity entity) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Locale getLocale() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setLocale(Locale loc) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean containsHeader(String name) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Header[] getHeaders(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Header getFirstHeader(String name) {
            if (name != null && name.equals("location")) {
                return new BasicHeader("location", "somewhere/put-code");
            }
            return null;
        }

        @Override
        public Header getLastHeader(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Header[] getAllHeaders() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addHeader(Header header) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void addHeader(String name, String value) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setHeader(Header header) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setHeader(String name, String value) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setHeaders(Header[] headers) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void removeHeader(Header header) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void removeHeaders(String name) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public HeaderIterator headerIterator() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HeaderIterator headerIterator(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HttpParams getParams() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setParams(HttpParams params) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
            
        }
        
    }

}
