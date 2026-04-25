package org.orcid.mp.assertion.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.orcid.mp.assertion.config.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {OrcidApiClient.class, CacheConfig.class})
public class OrcidApiClientIT {

    @Autowired
    private OrcidApiClient orcidApiClient;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean(name = "orcidRestClient")
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.TOKEN_CACHE).clear();
    }

    @Test
    void exchangeToken_shouldUseCacheForDuplicateIdTokens() throws Exception {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class, Answers.RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        String jsonResponse = "{ \"access_token\": \"mocked-access-token\" }";
        ResponseEntity<String> mockEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(responseSpec.toEntity(String.class)).thenReturn(mockEntity);

        String idToken1 = "ID-TOKEN-A";
        String orcidId = "0000-0000-0000-0000";

        String result1 = orcidApiClient.exchangeToken(idToken1, orcidId);
        assertEquals("mocked-access-token", result1);

        // check client only invoked once
        verify(responseSpec, times(1)).toEntity(String.class);

        String result2 = orcidApiClient.exchangeToken(idToken1, orcidId);
        assertEquals("mocked-access-token", result2);

        // check client still only invoked once
        verify(responseSpec, times(1)).toEntity(String.class);
    }
}
