package org.orcid.mp.user.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

public class AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // Find the original /oauth2/authorize URL that Spring saved earlier
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);

        // If there's no saved request (e.g. user went directly to /login),
        // we send them to a default landing page
        String targetUrl = (savedRequest != null)
                ? savedRequest.getRedirectUrl()
                : "http://localhost:4200/";

        // Send a JSON response so Angular can handle it
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\": \"success\", \"redirectUrl\": \"" + targetUrl + "\"}");
    }
}
