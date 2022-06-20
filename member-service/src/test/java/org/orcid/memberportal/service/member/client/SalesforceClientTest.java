package org.orcid.memberportal.service.member.client;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.config.ApplicationProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SalesforceClientTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private CloseableHttpClient httpClient;

    @InjectMocks
    private SalesforceClient client;

    @Captor
    private ArgumentCaptor<HttpUriRequest> requestCaptor;

    @BeforeEach
    public void setUp() throws JAXBException {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetMemberDetails() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getSalesforceClientEndpoint()).thenReturn("microservice/");
        Mockito.when(applicationProperties.getSalesforceClientToken()).thenReturn("access-token");
        
        OrcidCloseableHttpResponse response = new OrcidCloseableHttpResponse();
        response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
        response.setEntity(getMemberDetailsEntity());
        
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);

        MemberDetails memberDetails = client.getMemberDetails("salesforceId");
        
        assertThat(memberDetails).isNotNull();
        assertThat(memberDetails.getName()).isEqualTo("test member details");
        assertThat(memberDetails.getPublicDisplayName()).isEqualTo("public display name");
        assertThat(memberDetails.getWebsite()).isEqualTo("https://website.com");
        assertThat(memberDetails.getMembershipStartDateString()).isEqualTo("2022-01-01");
        assertThat(memberDetails.getMembershipEndDateString()).isEqualTo("2027-01-01");
        assertThat(memberDetails.getPublicDisplayEmail()).isEqualTo("orcid@testmember.com");
        assertThat(memberDetails.getConsortiaLeadId()).isNull();
        assertThat(memberDetails.isConsortiaMember()).isFalse();
        assertThat(memberDetails.getPublicDisplayDescriptionHtml()).isEqualTo("<p>public display description</p>");
        assertThat(memberDetails.getMemberType()).isEqualTo("Research Institute");
        assertThat(memberDetails.getLogoUrl()).isEqualTo("some/url/for/a/logo");
        assertThat(memberDetails.getBillingCountry()).isEqualTo("Denmark");
        assertThat(memberDetails.getId()).isEqualTo("id");
        
        Mockito.verify(applicationProperties).getSalesforceClientEndpoint();
        Mockito.verify(applicationProperties).getSalesforceClientToken();

    }
    
    @Test
    void testGetMemberDetails_noEndpoint() throws IOException  {
        Mockito.when(applicationProperties.getSalesforceClientEndpoint()).thenReturn(null);
        assertThat(client.getMemberDetails("salesforceId")).isNull();
        Mockito.verify(applicationProperties).getSalesforceClientEndpoint();
    }

    private HttpEntity getMemberDetailsEntity() throws JsonProcessingException, UnsupportedEncodingException {
        MemberDetails memberDetails = new MemberDetails();
        memberDetails.setBillingCountry("Denmark");
        memberDetails.setConsortiaLeadId(null);
        memberDetails.setConsortiaMember(false);
        memberDetails.setId("id");
        memberDetails.setLogoUrl("some/url/for/a/logo");
        memberDetails.setMemberType("Research Institute");
        memberDetails.setName("test member details");
        memberDetails.setPublicDisplayDescriptionHtml("<p>public display description</p>");
        memberDetails.setPublicDisplayEmail("orcid@testmember.com");
        memberDetails.setPublicDisplayName("public display name");
        memberDetails.setMembershipStartDateString("2022-01-01");
        memberDetails.setMembershipEndDateString("2027-01-01");
        memberDetails.setWebsite("https://website.com");
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        String jsonString = objectMapper.writeValueAsString(memberDetails);
        return new StringEntity(jsonString);
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
