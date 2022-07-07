package io.github.jhipster.registry.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import javax.xml.bind.JAXBException;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import io.github.jhipster.registry.service.dto.HealthDTO;

public class HealthClientTest {

    @Mock
    private CloseableHttpClient httpClient;

    @InjectMocks
    private HealthClient client;

    @BeforeEach
    public void setUp() throws JAXBException {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetHelp() throws JAXBException, ClientProtocolException, IOException {
        OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
        response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
        BasicHttpEntity orcidIdEntity = new BasicHttpEntity();
        orcidIdEntity.setContent(new ByteArrayInputStream("{\"status\":\"UP\"}".getBytes()));
        response.setEntity(orcidIdEntity);
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        
        HealthDTO health = client.getHealth("some-url");
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
    
    @Test
    void testGetHelpErrorResponse() throws JAXBException, ClientProtocolException, IOException {
        OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
        response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 500, "Internal Server Error"));
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        
        HealthDTO health = client.getHealth("some-url");
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
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
