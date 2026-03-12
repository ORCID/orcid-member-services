package org.orcid.mp.assertion.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.orcid.mp.assertion.domain.Member;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberServiceClientTest {

    private MemberServiceClient memberServiceClient;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        memberServiceClient = new MemberServiceClient();
        ReflectionTestUtils.setField(memberServiceClient, "restClient", restClient);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testGetMember() {
        Member mockMember = new Member();
        when(responseSpec.toEntity(Member.class)).thenReturn(new ResponseEntity<>(mockMember, HttpStatus.OK));

        Member result = memberServiceClient.getMember("123");

        assertThat(result).isEqualTo(mockMember);
        verify(requestHeadersUriSpec).uri("/members/123");
    }

    @Test
    void testUpdateMemberDefaultLanguage() {
        String expectedResponse = "Success";
        when(responseSpec.toEntity(String.class)).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        String result = memberServiceClient.updateMemberDefaultLanguage("salesforceId", "en");

        assertThat(result).isEqualTo(expectedResponse);
        verify(requestBodyUriSpec).uri("/members/salesforceId/language/en");
    }
}