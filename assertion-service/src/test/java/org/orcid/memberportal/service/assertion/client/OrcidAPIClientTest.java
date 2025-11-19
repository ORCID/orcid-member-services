package org.orcid.memberportal.service.assertion.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.web.rest.errors.DeactivatedException;
import org.orcid.memberportal.service.assertion.web.rest.errors.DeprecatedException;
import org.orcid.memberportal.service.assertion.web.rest.errors.ORCIDAPIException;

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

    @Test
    void testPostAffiliation() throws IOException, DeprecatedException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"last-modified-date\":{\"value\":1728652976786},\"name\":{\"created-date\":{\"value\":1482344662307},\"last-modified-date\":{\"value\":1482344662307},\"given-names\":{\"value\":\"A\"},\"family-name\":{\"value\":\"Person\"},\"credit-name\":null,\"source\":null,\"visibility\":\"public\",\"path\":\"0000-0001-0002-0003\"},\"other-names\":{\"last-modified-date\":null,\"other-name\":[],\"path\":\"/0000-0001-0002-0003/other-names\"},\"biography\":null,\"researcher-urls\":{\"last-modified-date\":null,\"researcher-url\":[],\"path\":\"/0000-0001-0002-0003/researcher-urls\"},\"emails\":{\"last-modified-date\":null,\"email\":[],\"path\":\"/0000-0001-0002-0003/email\"},\"addresses\":{\"last-modified-date\":null,\"address\":[],\"path\":\"/0000-0001-0002-0003/address\"},\"keywords\":{\"last-modified-date\":{\"value\":1728652976786},\"keyword\":[{\"created-date\":{\"value\":1728652976786},\"last-modified-date\":{\"value\":1728652976786},\"source\":{\"source-orcid\":{\"uri\":\"https://qa.orcid.org/0000-0001-0002-0003\",\"path\":\"0000-0001-0002-0003\",\"host\":\"qa.orcid.org\"},\"source-client-id\":null,\"source-name\":{\"value\":\"A Person\"},\"assertion-origin-orcid\":null,\"assertion-origin-client-id\":null,\"assertion-origin-name\":null},\"content\":\"jhj\",\"visibility\":\"public\",\"path\":\"/0000-0001-0002-0003/keywords/9691\",\"put-code\":9691,\"display-index\":1}],\"path\":\"/0000-0001-0002-0003/keywords\"},\"external-identifiers\":{\"last-modified-date\":null,\"external-identifier\":[],\"path\":\"/0000-0001-0002-0003/external-identifiers\"},\"path\":\"/0000-0001-0002-0003/person\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 201, "CREATED"));
                return response;
            }
        });

        assertThat(client.postAffiliation("orcid", "access-token", getAssertionWithEmail("hello@orcid.org"))).isEqualTo("put-code");
    }

    @Test
    void testPostAffiliation_recordDeprecated() throws IOException, DeprecatedException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"error\":\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalone=\\\"yes\\\"?>\\n<error xmlns=\\\"http://www.orcid.org/ns/error\\\">\\n    <response-code>409<\\/response-code>\\n    <developer-message>This account is deprecated. Please refer to account: https://qa.orcid.org/0000-0002-4563-570X.<\\/developer-message>\\n    <user-message>This account is deprecated. Please refer to account: https://qa.orcid.org/0000-0002-4563-570X.<\\/user-message>\\n    <error-code>9007<\\/error-code>\\n    <more-info>https://members.orcid.org/api/resources/troubleshooting<\\/more-info>\\n<\\/error>\\n\",\"statusCode\":409}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 409, "CONFLICT"));
                return response;
            }
        });
        assertThatExceptionOfType(DeprecatedException.class).isThrownBy(() -> {
            client.postAffiliation("orcid", "access-token", getAssertionWithEmail("hello@orcid.org"));
        });
    }

    @Test
    void testPostAffiliation_ioException() throws IOException, DeprecatedException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.doThrow(new IOException("something wrong")).when(httpClient).execute(Mockito.any(HttpUriRequest.class));
        assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
            client.postAffiliation("orcid", "access-token", getAssertionWithEmail("hello@orcid.org"));
        });
    }

    @Test
    void testPutAffiliation() throws IOException, DeprecatedException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"last-modified-date\":{\"value\":1728652976786},\"name\":{\"created-date\":{\"value\":1482344662307},\"last-modified-date\":{\"value\":1482344662307},\"given-names\":{\"value\":\"A\"},\"family-name\":{\"value\":\"Person\"},\"credit-name\":null,\"source\":null,\"visibility\":\"public\",\"path\":\"0000-0001-0002-0003\"},\"other-names\":{\"last-modified-date\":null,\"other-name\":[],\"path\":\"/0000-0001-0002-0003/other-names\"},\"biography\":null,\"researcher-urls\":{\"last-modified-date\":null,\"researcher-url\":[],\"path\":\"/0000-0001-0002-0003/researcher-urls\"},\"emails\":{\"last-modified-date\":null,\"email\":[],\"path\":\"/0000-0001-0002-0003/email\"},\"addresses\":{\"last-modified-date\":null,\"address\":[],\"path\":\"/0000-0001-0002-0003/address\"},\"keywords\":{\"last-modified-date\":{\"value\":1728652976786},\"keyword\":[{\"created-date\":{\"value\":1728652976786},\"last-modified-date\":{\"value\":1728652976786},\"source\":{\"source-orcid\":{\"uri\":\"https://qa.orcid.org/0000-0001-0002-0003\",\"path\":\"0000-0001-0002-0003\",\"host\":\"qa.orcid.org\"},\"source-client-id\":null,\"source-name\":{\"value\":\"A Person\"},\"assertion-origin-orcid\":null,\"assertion-origin-client-id\":null,\"assertion-origin-name\":null},\"content\":\"jhj\",\"visibility\":\"public\",\"path\":\"/0000-0001-0002-0003/keywords/9691\",\"put-code\":9691,\"display-index\":1}],\"path\":\"/0000-0001-0002-0003/keywords\"},\"external-identifiers\":{\"last-modified-date\":null,\"external-identifier\":[],\"path\":\"/0000-0001-0002-0003/external-identifiers\"},\"path\":\"/0000-0001-0002-0003/person\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                return response;
            }
        });

        client.putAffiliation("orcid", "access-token", getAssertionWithEmail("hello@orcid.org"));
    }

    @Test
    void testPutAffiliation_deprecatedProfile() throws IOException, DeprecatedException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"error\":\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalone=\\\"yes\\\"?>\\n<error xmlns=\\\"http://www.orcid.org/ns/error\\\">\\n    <response-code>409<\\/response-code>\\n    <developer-message>This account is deprecated. Please refer to account: https://qa.orcid.org/0000-0002-4563-570X.<\\/developer-message>\\n    <user-message>This account is deprecated. Please refer to account: https://qa.orcid.org/0000-0002-4563-570X.<\\/user-message>\\n    <error-code>9007<\\/error-code>\\n    <more-info>https://members.orcid.org/api/resources/troubleshooting<\\/more-info>\\n<\\/error>\\n\",\"statusCode\":409}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 409, "CONFLICT"));
                return response;
            }
        });
        assertThatExceptionOfType(DeprecatedException.class).isThrownBy(() -> {
            client.putAffiliation("orcid", "access-token", getAssertionWithEmail("hello@orcid.org"));
        });
    }

    @Test
    void testDeleteAffiliation() throws IOException, DeprecatedException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"last-modified-date\":{\"value\":1728652976786},\"name\":{\"created-date\":{\"value\":1482344662307},\"last-modified-date\":{\"value\":1482344662307},\"given-names\":{\"value\":\"A\"},\"family-name\":{\"value\":\"Person\"},\"credit-name\":null,\"source\":null,\"visibility\":\"public\",\"path\":\"0000-0001-0002-0003\"},\"other-names\":{\"last-modified-date\":null,\"other-name\":[],\"path\":\"/0000-0001-0002-0003/other-names\"},\"biography\":null,\"researcher-urls\":{\"last-modified-date\":null,\"researcher-url\":[],\"path\":\"/0000-0001-0002-0003/researcher-urls\"},\"emails\":{\"last-modified-date\":null,\"email\":[],\"path\":\"/0000-0001-0002-0003/email\"},\"addresses\":{\"last-modified-date\":null,\"address\":[],\"path\":\"/0000-0001-0002-0003/address\"},\"keywords\":{\"last-modified-date\":{\"value\":1728652976786},\"keyword\":[{\"created-date\":{\"value\":1728652976786},\"last-modified-date\":{\"value\":1728652976786},\"source\":{\"source-orcid\":{\"uri\":\"https://qa.orcid.org/0000-0001-0002-0003\",\"path\":\"0000-0001-0002-0003\",\"host\":\"qa.orcid.org\"},\"source-client-id\":null,\"source-name\":{\"value\":\"A Person\"},\"assertion-origin-orcid\":null,\"assertion-origin-client-id\":null,\"assertion-origin-name\":null},\"content\":\"jhj\",\"visibility\":\"public\",\"path\":\"/0000-0001-0002-0003/keywords/9691\",\"put-code\":9691,\"display-index\":1}],\"path\":\"/0000-0001-0002-0003/keywords\"},\"external-identifiers\":{\"last-modified-date\":null,\"external-identifier\":[],\"path\":\"/0000-0001-0002-0003/external-identifiers\"},\"path\":\"/0000-0001-0002-0003/person\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 204, "NO CONTENT"));
                return response;
            }
        });

        client.deleteAffiliation("orcid", "access-token", getAssertionWithEmail("hello@orcid.org"));
    }

    @Test
    void testDeleteAffiliation_deactivatedProfile() throws IOException, DeprecatedException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"error\":\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalone=\\\"yes\\\"?>\\n<error xmlns=\\\"http://www.orcid.org/ns/error\\\">\\n    <response-code>409<\\/response-code>\\n    <developer-message>This account is deprecated. Please refer to account: https://qa.orcid.org/0000-0002-4563-570X.<\\/developer-message>\\n    <user-message>This account is deprecated. Please refer to account: https://qa.orcid.org/0000-0002-4563-570X.<\\/user-message>\\n    <error-code>9007<\\/error-code>\\n    <more-info>https://members.orcid.org/api/resources/troubleshooting<\\/more-info>\\n<\\/error>\\n\",\"statusCode\":409}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 409, "CONFLICT"));
                return response;
            }
        });
        assertThatExceptionOfType(DeprecatedException.class).isThrownBy(() -> {
            client.deleteAffiliation("orcid", "access-token", getAssertionWithEmail("hello@orcid.org"));
        });
    }

    private Assertion getAssertionWithEmail(String email) {
        Assertion assertion = new Assertion();
        assertion.setEmail(email);
        assertion.setSalesforceId("salesforce-id");
        assertion.setAffiliationSection(AffiliationSection.EMPLOYMENT);
        assertion.setDepartmentName("department");
        assertion.setOrgCity("city");
        assertion.setOrgName("org");
        assertion.setOrgRegion("region");
        assertion.setOrgCountry("US");
        assertion.setDisambiguatedOrgId("id");
        assertion.setDisambiguationSource("source");
        assertion.setStatus(AssertionStatus.PENDING.name());
        return assertion;
    }

    @Test
    void testExchangeToken_deactivatedRecord() throws IOException {
        TokenExchange mockTokenExchange = Mockito.mock(TokenExchange.class);
        Mockito.when(mockTokenExchange.getEndpoint()).thenReturn("orcid/tokenexchange/");
        Mockito.when(applicationProperties.getTokenExchange()).thenReturn(mockTokenExchange);
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 401, "Unauthorized"));
                String tokenResponse = "{“ error\": \"invalid_scope\", \"error_description\": \"The id_token is disabled and does not contain any valid scope\" }";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"internal-access-token\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 409, "CONFLICT"));
                String body = "{\"response-code\":409,\"developer-message\":\"409 Conflict: The ORCID record is deactivated and cannot be edited. Full validation error: 0009-0004-5673-641X is deactivated\",\"user-message\":\"The ORCID record is deactivated.\",\"error-code\":9044,\"more-info\":\"https://members.orcid.org/api/resources/troubleshooting\"}";
                StringEntity entity = new StringEntity(body, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        });

        assertThatExceptionOfType(DeactivatedException.class).isThrownBy(() -> {
           client.exchangeToken("some id token", "orcid");
        });
    }

    @Test
    void testExchangeToken_orcidApiError() throws IOException {
        TokenExchange mockTokenExchange = Mockito.mock(TokenExchange.class);
        Mockito.when(mockTokenExchange.getEndpoint()).thenReturn("orcid/tokenexchange/");
        Mockito.when(applicationProperties.getTokenExchange()).thenReturn(mockTokenExchange);
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 401, "Unauthorized"));
                String tokenResponse = "{“ error\": \"invalid_scope\", \"error_description\": \"The id_token is disabled and does not contain any valid scope\" }";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"internal-access-token\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-internal /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "CONFLICT"));
                String body = "{\"last-modified-date\":{\"value\":1728652976786},\"name\":{\"created-date\":{\"value\":1482344662307},\"last-modified-date\":{\"value\":1482344662307},\"given-names\":{\"value\":\"George\"},\"family-name\":{\"value\":\"Nash\"},\"credit-name\":null,\"source\":null,\"visibility\":\"public\",\"path\":\"0000-0001-0002-0003\"},\"other-names\":{\"last-modified-date\":null,\"other-name\":[],\"path\":\"/0000-0001-0002-0003/other-names\"},\"biography\":null,\"researcher-urls\":{\"last-modified-date\":null,\"researcher-url\":[],\"path\":\"/0000-0001-0002-0003/researcher-urls\"},\"emails\":{\"last-modified-date\":null,\"email\":[],\"path\":\"/0000-0001-0002-0003/email\"},\"addresses\":{\"last-modified-date\":null,\"address\":[],\"path\":\"/0000-0001-0002-0003/address\"},\"keywords\":{\"last-modified-date\":{\"value\":1728652976786},\"keyword\":[{\"created-date\":{\"value\":1728652976786},\"last-modified-date\":{\"value\":1728652976786},\"source\":{\"source-orcid\":{\"uri\":\"https://qa.orcid.org/0000-0001-0002-0003\",\"path\":\"0000-0001-0002-0003\",\"host\":\"qa.orcid.org\"},\"source-client-id\":null,\"source-name\":{\"value\":\"George Nash\"},\"assertion-origin-orcid\":null,\"assertion-origin-client-id\":null,\"assertion-origin-name\":null},\"content\":\"jhj\",\"visibility\":\"public\",\"path\":\"/0000-0001-0002-0003/keywords/9691\",\"put-code\":9691,\"display-index\":1}],\"path\":\"/0000-0001-0002-0003/keywords\"},\"external-identifiers\":{\"last-modified-date\":null,\"external-identifier\":[],\"path\":\"/0000-0001-0002-0003/external-identifiers\"},\"path\":\"/0000-0001-0002-0003/person\"}";
                StringEntity entity = new StringEntity(body, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        });

        assertThatExceptionOfType(ORCIDAPIException.class).isThrownBy(() -> {
            client.exchangeToken("some id token", "0000-0001-0002-0003");
        });
    }

    @Test
    void testRecordIsDeactivated_recordDeactivated() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"token\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-api /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"response-code\":409,\"developer-message\":\"409 Conflict: The ORCID record is deactivated and cannot be edited. Full validation error: 0009-0004-5673-641X is deactivated\",\"user-message\":\"The ORCID record is deactivated.\",\"error-code\":9044,\"more-info\":\"https://members.orcid.org/api/resources/troubleshooting\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 409, "CONFLICT"));
                return response;
            }
        });

        boolean deprecatedOrDeactivated = client.recordIsDeactivated("1234-1234-1234-1234");
        assertThat(deprecatedOrDeactivated).isEqualTo(true);
    }

    @Test
    void testRecordIsDeactivated_tokenExpired() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"expired-token\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-api /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"response-code\":401,\"developer-message\":\"401 Unauthorized: Token expired message\",\"user-message\":\"token expired.\",\"error-code\":9044,\"more-info\":\"https://members.orcid.org/api/resources/troubleshooting\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 401, "CONFLICT"));
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"expired-token\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-api /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"response-code\":409,\"developer-message\":\"409 Conflict: The ORCID record is deactivated and cannot be edited. Full validation error: 0009-0004-5673-641X is deactivated\",\"user-message\":\"The ORCID record is deactivated.\",\"error-code\":9044,\"more-info\":\"https://members.orcid.org/api/resources/troubleshooting\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 409, "CONFLICT"));
                return response;
            }
        });
        // first token expired, new token obtained
        boolean deprecatedOrDeactivated = client.recordIsDeactivated("1234-1234-1234-1234");
        assertThat(deprecatedOrDeactivated).isEqualTo(true);

        // two requests for tokens due to one expired, two checks for deactivated record
        Mockito.verify(httpClient, Mockito.times(4)).execute(Mockito.any(HttpUriRequest.class));
    }

    @Test
    void testRecordIsDeactivated_recordNotDeactivated() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getOrcidAPIEndpoint()).thenReturn("orcid/api/");
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                String tokenResponse = "{\"access_token\":\"token\",\"token_type\":\"bearer\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":3599,\"scope\":\"/orcid-api /premium-notification\",\"orcid\":null}";
                StringEntity entity = new StringEntity(tokenResponse, "UTF-8");
                entity.setContentType("application/json;charset=UTF-8");
                response.setEntity(entity);
                return response;
            }
        }).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
                response.setEntity(new StringEntity("{\"last-modified-date\":{\"value\":1728652976786},\"name\":{\"created-date\":{\"value\":1482344662307},\"last-modified-date\":{\"value\":1482344662307},\"given-names\":{\"value\":\"A\"},\"family-name\":{\"value\":\"Person\"},\"credit-name\":null,\"source\":null,\"visibility\":\"public\",\"path\":\"0000-0001-0002-0003\"},\"other-names\":{\"last-modified-date\":null,\"other-name\":[],\"path\":\"/0000-0001-0002-0003/other-names\"},\"biography\":null,\"researcher-urls\":{\"last-modified-date\":null,\"researcher-url\":[],\"path\":\"/0000-0001-0002-0003/researcher-urls\"},\"emails\":{\"last-modified-date\":null,\"email\":[],\"path\":\"/0000-0001-0002-0003/email\"},\"addresses\":{\"last-modified-date\":null,\"address\":[],\"path\":\"/0000-0001-0002-0003/address\"},\"keywords\":{\"last-modified-date\":{\"value\":1728652976786},\"keyword\":[{\"created-date\":{\"value\":1728652976786},\"last-modified-date\":{\"value\":1728652976786},\"source\":{\"source-orcid\":{\"uri\":\"https://qa.orcid.org/0000-0001-0002-0003\",\"path\":\"0000-0001-0002-0003\",\"host\":\"qa.orcid.org\"},\"source-client-id\":null,\"source-name\":{\"value\":\"A Person\"},\"assertion-origin-orcid\":null,\"assertion-origin-client-id\":null,\"assertion-origin-name\":null},\"content\":\"jhj\",\"visibility\":\"public\",\"path\":\"/0000-0001-0002-0003/keywords/9691\",\"put-code\":9691,\"display-index\":1}],\"path\":\"/0000-0001-0002-0003/keywords\"},\"external-identifiers\":{\"last-modified-date\":null,\"external-identifier\":[],\"path\":\"/0000-0001-0002-0003/external-identifiers\"},\"path\":\"/0000-0001-0002-0003/person\"}"));
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                return response;
            }
        });

        boolean deprecatedOrDeactivated = client.recordIsDeactivated("0000-0001-0002-0003");
        assertThat(deprecatedOrDeactivated).isEqualTo(false);
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
