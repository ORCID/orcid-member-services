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
class InternalMemberServiceClientTest {

    private InternalMemberServiceClient internalMemberServiceClient;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        internalMemberServiceClient = new InternalMemberServiceClient();
        ReflectionTestUtils.setField(internalMemberServiceClient, "internalMemberServiceRestClient", restClient);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testGetMember() {
        Member mockMember = new Member();
        mockMember.setClientId("123");

        when(responseSpec.toEntity(Member.class)).thenReturn(new ResponseEntity<>(mockMember, HttpStatus.OK));

        Member result = internalMemberServiceClient.getMember("123");

        assertThat(result).isEqualTo(mockMember);
        verify(requestHeadersUriSpec).uri("/internal/members/123");
    }
}
