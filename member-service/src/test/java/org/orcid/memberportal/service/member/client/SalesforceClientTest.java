package org.orcid.memberportal.service.member.client;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
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
import org.orcid.memberportal.service.member.client.model.ConsortiumLeadDetails;
import org.orcid.memberportal.service.member.client.model.ConsortiumMember;
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.config.ApplicationProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        Mockito.when(applicationProperties.getOrcidApiTokenEndpoint()).thenReturn("orcid.org/oauth/token/");
    }

    @Test
    void testGetMemberDetails() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getSalesforceClientEndpoint()).thenReturn("microservice/");
        
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
            	MockCloseableHttpResponse response = new MockCloseableHttpResponse();
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
            	MockCloseableHttpResponse response = new MockCloseableHttpResponse();
            	response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                response.setEntity(getMemberDetailsEntity());
                return response;
            }
        });
        

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

    }
    
    @Test
    void testGetConsortiumLeadDetails() throws JAXBException, ClientProtocolException, IOException {
        Mockito.when(applicationProperties.getSalesforceClientEndpoint()).thenReturn("microservice/");

        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                // request for orcid internal token
            	MockCloseableHttpResponse response = new MockCloseableHttpResponse();
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
            	MockCloseableHttpResponse response = new MockCloseableHttpResponse();
                response.setStatusLine(new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), 200, "OK"));
                response.setEntity(getConsortiumLeadDetailsEntity());
                return response;
            }
        });
        
        ConsortiumLeadDetails consortiumLeadDetails = client.getConsortiumLeadDetails("salesforceId");
        
        assertThat(consortiumLeadDetails).isNotNull();
        assertThat(consortiumLeadDetails.getName()).isEqualTo("test consortium lead details");
        assertThat(consortiumLeadDetails.getPublicDisplayName()).isEqualTo("public display name");
        assertThat(consortiumLeadDetails.getWebsite()).isEqualTo("https://website.com");
        assertThat(consortiumLeadDetails.getMembershipStartDateString()).isEqualTo("2022-01-01");
        assertThat(consortiumLeadDetails.getMembershipEndDateString()).isEqualTo("2027-01-01");
        assertThat(consortiumLeadDetails.getPublicDisplayEmail()).isEqualTo("orcid@testmember.com");
        assertThat(consortiumLeadDetails.getConsortiaLeadId()).isNull();
        assertThat(consortiumLeadDetails.isConsortiaMember()).isFalse();
        assertThat(consortiumLeadDetails.getPublicDisplayDescriptionHtml()).isEqualTo("<p>public display description</p>");
        assertThat(consortiumLeadDetails.getMemberType()).isEqualTo("Research Institute");
        assertThat(consortiumLeadDetails.getLogoUrl()).isEqualTo("some/url/for/a/logo");
        assertThat(consortiumLeadDetails.getBillingCountry()).isEqualTo("Denmark");
        assertThat(consortiumLeadDetails.getId()).isEqualTo("id");
        assertThat(consortiumLeadDetails.getConsortiumMembers().size()).isEqualTo(2);
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(0).getSalesforceId()).isEqualTo("member1");
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(0).getMetadata().getName()).isEqualTo("member 1");
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(1).getSalesforceId()).isEqualTo("member2");
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(1).getMetadata().getName()).isEqualTo("member 2");
        
        Mockito.verify(applicationProperties).getSalesforceClientEndpoint();
    }
    
    private HttpEntity getConsortiumLeadDetailsEntity() throws JsonProcessingException, UnsupportedEncodingException {
        ConsortiumLeadDetails consortiumLeadDetails = new ConsortiumLeadDetails();
        consortiumLeadDetails.setBillingCountry("Denmark");
        consortiumLeadDetails.setConsortiaLeadId(null);
        consortiumLeadDetails.setConsortiaMember(false);
        consortiumLeadDetails.setId("id");
        consortiumLeadDetails.setLogoUrl("some/url/for/a/logo");
        consortiumLeadDetails.setMemberType("Research Institute");
        consortiumLeadDetails.setName("test consortium lead details");
        consortiumLeadDetails.setPublicDisplayDescriptionHtml("<p>public display description</p>");
        consortiumLeadDetails.setPublicDisplayEmail("orcid@testmember.com");
        consortiumLeadDetails.setPublicDisplayName("public display name");
        consortiumLeadDetails.setMembershipStartDateString("2022-01-01");
        consortiumLeadDetails.setMembershipEndDateString("2027-01-01");
        consortiumLeadDetails.setWebsite("https://website.com");
        
        MemberDetailsResponseEntity response = new MemberDetailsResponseEntity();
        response.setMember(consortiumLeadDetails);

        ConsortiumMember member1 = new ConsortiumMember();
        member1.setSalesforceId("member1");
        ConsortiumMember.Metadata metadata1 = member1.new Metadata();
        metadata1.setName("member 1");
        member1.setMetadata(metadata1);
        
        ConsortiumMember member2 = new ConsortiumMember();
        member2.setSalesforceId("member2");
        ConsortiumMember.Metadata metadata2 = member2.new Metadata();
        metadata2.setName("member 2");
        member2.setMetadata(metadata2);
        
        response.setConsortiumMembers(Arrays.asList(member1, member2));
        
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(response);
        return new StringEntity(jsonString);
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
        
        MemberDetailsResponseEntity response = new MemberDetailsResponseEntity();
        response.setMember(memberDetails);
        
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(response);
        return new StringEntity(jsonString);
    }
    
}
