package org.orcid.memberportal.service.gateway.security.oauth2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orcid.memberportal.service.gateway.security.oauth2.CookieTokenExtractor;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Test whether the {@link CookieTokenExtractor} can properly extract access
 * tokens from Cookies and Headers.
 */
public class CookieTokenExtractorTest {
    private CookieTokenExtractor cookieTokenExtractor;

    @BeforeEach
    public void init() {
        cookieTokenExtractor = new CookieTokenExtractor();
    }

    @Test
    public void testExtractTokenCookie() {
        MockHttpServletRequest request = OAuth2AuthenticationServiceTest.createMockHttpServletRequest();
        Authentication authentication = cookieTokenExtractor.extract(request);
        Assertions.assertEquals(OAuth2AuthenticationServiceTest.ACCESS_TOKEN_VALUE, authentication.getPrincipal().toString());
    }

    @Test
    public void testExtractTokenHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "http://www.test.com");
        request.addHeader("Authorization", OAuth2AccessToken.BEARER_TYPE + " " + OAuth2AuthenticationServiceTest.ACCESS_TOKEN_VALUE);
        Authentication authentication = cookieTokenExtractor.extract(request);
        Assertions.assertEquals(OAuth2AuthenticationServiceTest.ACCESS_TOKEN_VALUE, authentication.getPrincipal().toString());
    }

    @Test
    public void testExtractTokenParam() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "http://www.test.com");
        request.addParameter(OAuth2AccessToken.ACCESS_TOKEN, OAuth2AuthenticationServiceTest.ACCESS_TOKEN_VALUE);
        Authentication authentication = cookieTokenExtractor.extract(request);
        Assertions.assertEquals(OAuth2AuthenticationServiceTest.ACCESS_TOKEN_VALUE, authentication.getPrincipal().toString());
    }
}
