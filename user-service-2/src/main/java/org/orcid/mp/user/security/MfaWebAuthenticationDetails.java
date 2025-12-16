package org.orcid.mp.user.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class MfaWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String mfaCode;

    public MfaWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.mfaCode = request.getParameter("mfa_code");
    }

    public String getMfaCode() {
        return mfaCode;
    }
}
