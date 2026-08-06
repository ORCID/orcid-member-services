package org.orcid.mp.user.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

public class MfaAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");

        if (exception instanceof MfaRequiredException) {
            response.getWriter().write("{\"error\": \"mfa_required\", \"message\": \"Please provide MFA code\"}");
        } else if (exception instanceof MfaInvalidCodeException) {
            response.getWriter().write("{\"error\": \"mfa_invalid\", \"message\": \"Invalid MFA code\"}");
        } else if (exception instanceof DeactivatedMemberException) {
            response.getWriter().write("{\"error\": \"deactivated_member\", \"message\": \"Your organization is not an active ORCID member. Please contact membership@orcid.org to reactivate your membership.\"}");
        } else {
            response.getWriter().write("{\"error\": \"invalid_credentials\", \"message\": \"Invalid sign in credentials. Please check your email and password and try again.\"}");
        }
        response.getWriter().flush();
    }
}