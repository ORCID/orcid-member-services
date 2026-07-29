package org.orcid.mp.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MfaAuthenticationFailureHandlerTest {

    private final MfaAuthenticationFailureHandler failureHandler = new MfaAuthenticationFailureHandler();

    @Test
    void onAuthenticationFailure_DeactivatedMember_ReturnsUpdatedMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new DeactivatedMemberException());

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals(
                "{\"error\": \"deactivated_member\", \"message\": \"Your organization is not an active ORCID member. Please contact membership@orcid.org to reactivate your membership.\"}",
                response.getContentAsString()
        );
    }

    @Test
    void onAuthenticationFailure_InvalidCredentials_ReturnsUpdatedMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("bad credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals(
                "{\"error\": \"invalid_credentials\", \"message\": \"Invalid sign in credentials. Please check your email and password and try again.\"}",
                response.getContentAsString()
        );
    }
}
