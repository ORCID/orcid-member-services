package org.orcid.memberportal.service.user.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.orcid.memberportal.service.user.services.UserService;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;

public class PasswordTokenGranter extends AbstractTokenGranter {

    private static final String GRANT_TYPE = "password";

    private static final GrantedAuthority PRE_AUTH = new SimpleGrantedAuthority("PRE_AUTH");

    private AuthenticationManager authenticationManager;

    private AuthorizationServerEndpointsConfigurer endpoints;

    private UserService userService;

    private DefaultTokenServices tokenServices;

    private ClientDetailsService clientDetailsService;

    public PasswordTokenGranter(AuthorizationServerEndpointsConfigurer endpoints, AuthenticationManager authenticationManager, UserService userService,
            DefaultTokenServices tokenServices) {
        super(tokenServices, endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory(), GRANT_TYPE);
        this.authenticationManager = authenticationManager;
        this.endpoints = endpoints;
        this.userService = userService;
        this.tokenServices = tokenServices;
        this.clientDetailsService = endpoints.getClientDetailsService();
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        if (grantType.equals(GRANT_TYPE)) {
            Map<String, String> parameters = new LinkedHashMap<>(tokenRequest.getRequestParameters());
            String username = parameters.get("username");
            String password = parameters.get("password");
            String mfaCode = parameters.get("mfa_code");
            parameters.remove("password");

            Authentication userAuth = new UsernamePasswordAuthenticationToken(username, password);
            ((AbstractAuthenticationToken) userAuth).setDetails(parameters);
            String clientId = tokenRequest.getClientId();
            ClientDetails client = this.clientDetailsService.loadClientByClientId(clientId);
            this.validateGrantType(grantType, client);
            try {
                userAuth = this.authenticationManager.authenticate(userAuth);
            } catch (AccountStatusException | BadCredentialsException e) {
                throw new InvalidGrantException(e.getMessage());
            }
            if (userAuth != null && userAuth.isAuthenticated()) {
                OAuth2Request storedOAuth2Request = this.getRequestFactory().createOAuth2Request(client, tokenRequest);
                if (mfaCodeRequired(username, mfaCode)) {
                    userAuth = new UsernamePasswordAuthenticationToken(username, password, Collections.singleton(PRE_AUTH));
                    OAuth2AccessToken accessToken = this.endpoints.getTokenServices().createAccessToken(new OAuth2Authentication(storedOAuth2Request, userAuth));
                    return accessToken;
                }
                OAuth2AccessToken jwtToken = this.tokenServices.createAccessToken(new OAuth2Authentication(storedOAuth2Request, userAuth));
                return jwtToken;
            } else {
                throw new InvalidGrantException("Could not authenticate user: " + username);
            }
        } else {
            return null;
        }
    }

    private boolean mfaCodeRequired(String username, String mfaCode) {
        return userService.isMfaEnabled(username) && (StringUtils.isBlank(mfaCode) || !userService.validMfaCode(username, mfaCode));
    }

}
