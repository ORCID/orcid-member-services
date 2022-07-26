package org.orcid.memberportal.service.assertion.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
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
import org.apache.http.entity.StringEntity;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.jaxb.model.v3.release.error.OrcidError;
import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.Item;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.Items;
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
import org.orcid.memberportal.service.assertion.config.ApplicationProperties.TokenExchange;

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
    public void setUp() throws JAXBException, ClientProtocolException, IOException {
        MockitoAnnotations.initMocks(this);
        jaxbContext = JAXBContext.newInstance(Affiliation.class, Distinction.class, Employment.class, Education.class, InvitedPosition.class, Membership.class,
                Qualification.class, Service.class, OrcidError.class, NotificationPermission.class);
        unmarshaller = jaxbContext.createUnmarshaller();
        
        TokenExchange tokenExchange = new TokenExchange();
        tokenExchange.setClientId("client-id");
        tokenExchange.setClientSecret("client-secret");
        Mockito.when(applicationProperties.getTokenExchange()).thenReturn(tokenExchange);
    }        

    @Test
    void testPostNotification() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getURI().toString().endsWith("oauth/token")) {
                    // request for orcid internal token
                    OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                    response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                    String tokenResponse = "{\"access_token\":\"new-access-token\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                    StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                    entity.setContentType("application/json;charset=UTF-8");
                    response.setEntity(entity);
                    return response;
                } else {
                    OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                    response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 201, "CREATED"));
                    return response;
                }
            }
        });

        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/v3/");
        NotificationPermission notification = getNotificationPermission();
        String putCode = client.postNotification(notification, "orcid");

        assertThat(putCode).isEqualTo("put-code");

        Mockito.verify(applicationProperties).getOrcidAPIEndpoint();
        Mockito.verify(httpClient, Mockito.times(2)).execute(requestCaptor.capture());

        HttpUriRequest request = requestCaptor.getAllValues().get(0);
        assertThat(request.getURI().toString().endsWith("oauth/token"));

        request = requestCaptor.getAllValues().get(1);
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
        assertThat(notificationPermission.getItems().getItems().size()).isEqualTo(2);
        assertThat(notificationPermission.getItems().getItems().get(0).getItemName()).isEqualTo("name");
        assertThat(notificationPermission.getItems().getItems().get(0).getItemType()).isEqualTo(ItemType.DISTINCTION);
        assertThat(notificationPermission.getItems().getItems().get(1).getItemName()).isEqualTo("name 2");
        assertThat(notificationPermission.getItems().getItems().get(1).getItemType()).isEqualTo(ItemType.EDUCATION);
    }

    @Test
    void testPostNotification_tokenNotWorking() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"access-token-that-wont-work\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenThrow(new IOException("first token doesn't work")).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"access-token-that-will-work\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 201, "CREATED"));
                return response;
            }
        });

        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/v3/");
        NotificationPermission notification = getNotificationPermission();
        String putCode = client.postNotification(notification, "orcid");

        assertThat(putCode).isEqualTo("put-code");

        Mockito.verify(applicationProperties, Mockito.times(2)).getOrcidAPIEndpoint();
        Mockito.verify(httpClient, Mockito.times(4)).execute(requestCaptor.capture());

        HttpUriRequest request = requestCaptor.getAllValues().get(0);
        assertThat(request.getURI().toString().endsWith("oauth/token"));
        HttpEntityEnclosingRequest requestWithBody = (HttpEntityEnclosingRequest) request;
        HttpEntity entity = requestWithBody.getEntity();

        request = requestCaptor.getAllValues().get(1);
        assertThat(request.getURI().toString()).isEqualTo("orcid/v3/orcid/notification-permission");

        request = requestCaptor.getAllValues().get(2);
        assertThat(request.getURI().toString().endsWith("oauth/token"));
        requestWithBody = (HttpEntityEnclosingRequest) request;

        request = requestCaptor.getAllValues().get(3);
        requestWithBody = (HttpEntityEnclosingRequest) request;
        entity = requestWithBody.getEntity();
        assertThat(entity.getContentType().getName()).isEqualTo("Content-Type");
        assertThat(entity.getContentType().getValue()).startsWith("text/xml");

        NotificationPermission notificationPermission = (NotificationPermission) unmarshaller.unmarshal(entity.getContent());
        assertThat(notificationPermission.getNotificationSubject()).isEqualTo("subject");
        assertThat(notificationPermission.getNotificationType()).isEqualTo(NotificationType.PERMISSION);
        assertThat(notificationPermission.getNotificationIntro()).isEqualTo("intro");
        assertThat(notificationPermission.getItems()).isNotNull();
        assertThat(notificationPermission.getItems().getItems()).isNotNull();
        assertThat(notificationPermission.getItems().getItems().size()).isEqualTo(2);
        assertThat(notificationPermission.getItems().getItems().get(0).getItemName()).isEqualTo("name");
        assertThat(notificationPermission.getItems().getItems().get(0).getItemType()).isEqualTo(ItemType.DISTINCTION);
        assertThat(notificationPermission.getItems().getItems().get(1).getItemName()).isEqualTo("name 2");
        assertThat(notificationPermission.getItems().getItems().get(1).getItemType()).isEqualTo(ItemType.EDUCATION);
    }

    @Test
    void testGetOrcidIdForEmail() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getInternalRegistryApiEndpoint()).thenReturn("orcid/internal/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"access-token-that-wont-work\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"orcid\":\"1234-1234-1234-1234\",\"email\":\"a.email@orcid.org\",\"status\":\"FOUND\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                return response;
            }
        });

        String orcidId = client.getOrcidIdForEmail("a.email@orcid.org");
        assertThat(orcidId).isEqualTo("1234-1234-1234-1234");
    }

    @Test
    void testGetOrcidIdForEmail_firstTokenNotWorking() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getInternalRegistryApiEndpoint()).thenReturn("orcid/internal/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"access-token-that-wont-work\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenThrow(new IOException("first token doesn't work")).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"access-token-that-will-work\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"orcid\":\"1234-1234-1234-1234\",\"email\":\"a.email@orcid.org\",\"status\":\"FOUND\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                return response;
            }
        });

        String orcidId = client.getOrcidIdForEmail("a.email@orcid.org");
        assertThat(orcidId).isEqualTo("1234-1234-1234-1234");
        
        Mockito.verify(applicationProperties, Mockito.times(4)).getInternalRegistryApiEndpoint();
        Mockito.verify(httpClient, Mockito.times(4)).execute(requestCaptor.capture());

        HttpUriRequest request = requestCaptor.getAllValues().get(0);
        assertThat(request.getURI().toString().endsWith("oauth/token"));

        request = requestCaptor.getAllValues().get(1);
        assertThat(request.getURI().toString()).endsWith("/email");

        request = requestCaptor.getAllValues().get(2);
        assertThat(request.getURI().toString().endsWith("oauth/token"));

        request = requestCaptor.getAllValues().get(3);
        assertThat(request.getURI().toString()).endsWith("/email");
    }

    
    @Test
    void testGetOrcidIdForEmail_orcidIdNotFound() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getInternalRegistryApiEndpoint()).thenReturn("orcid/internal/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"access-token-that-wont-work\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"orcid\":\"\",\"email\":\"a.email@orcid.org\",\"status\":\"NOT_FOUND\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                return response;
            }
        });

        String orcidId = client.getOrcidIdForEmail("a.email@orcid.org");
        assertThat(orcidId).isNull();
    }

    private NotificationPermission getNotificationPermission() {
        NotificationPermission notification = new NotificationPermission();
        notification.setNotificationSubject("subject");
        notification.setNotificationIntro("intro");

        Item item = new Item();
        item.setItemName("name");
        item.setItemType(ItemType.DISTINCTION);

        Item item2 = new Item();
        item2.setItemName("name 2");
        item2.setItemType(ItemType.EDUCATION);

        notification.setItems(new Items(Arrays.asList(item, item2)));
        return notification;
    }

    private class OrcidCloseableHttpResponse implements CloseableHttpResponse {

        private HttpEntity entity;

        private StatusLine statusLine;

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

        public HttpEntity getEntity() {
            return entity;
        }

        public void setEntity(HttpEntity entity) {
            this.entity = entity;
        }

        public StatusLine getStatusLine() {
            return statusLine;
        }

        public void setStatusLine(StatusLine statusLine) {
            this.statusLine = statusLine;
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
