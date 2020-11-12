package org.orcid.user.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.util.Assert;


public class MSSwitchUserFilter extends SwitchUserFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MSSwitchUserFilter.class);
    
    private final UserDetailsService userDetailsService;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;
    private String targetUrl;
    private String switchFailureUrl;
    
    @Autowired
    private ClientDetailsService clientDetailsService;
    
    @Autowired
    private AuthorizationServerTokenServices tokenService;
    
    
    @Autowired
    private AuthorizationServerEndpointsConfiguration configuration;
    
    public MSSwitchUserFilter(UserDetailsService userDetailsService ) {
        this.userDetailsService = userDetailsService;
        
    }
    
    @Override
    public void afterPropertiesSet() {
            Assert.notNull(this.userDetailsService, "userDetailsService must be specified");
            Assert.isTrue(this.successHandler != null || this.targetUrl != null,
                            "You must set either a successHandler or the targetUrl");
            if (this.targetUrl != null) {
                    Assert.isNull(this.successHandler,
                                    "You cannot set both successHandler and targetUrl");
                    this.successHandler = new SimpleUrlAuthenticationSuccessHandler(
                                    this.targetUrl);
            }

            if (this.failureHandler == null) {
                    this.failureHandler = this.switchFailureUrl == null
                                    ? new SimpleUrlAuthenticationFailureHandler()
                                    : new SimpleUrlAuthenticationFailureHandler(this.switchFailureUrl);
            }
            else {
                    Assert.isNull(this.switchFailureUrl,
                                    "You cannot set both a switchFailureUrl and a failureHandler");
            }
    }
    
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        LOGGER.debug("!!!!! START doFilter MSSwitchUserFilter");
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // check for switch or exit request
        if (requiresSwitchUser(request)) {
                // if set, attempt switch and store original
                try {
                        Authentication targetUser = attemptSwitchUser(request);

                        // update the current context to the new target user
                        SecurityContextHolder.getContext().setAuthentication(targetUser);
                        LOGGER.debug("!!! Get from context: " + SecurityContextHolder.getContext().getAuthentication().toString() );
                        UsernamePasswordAuthenticationToken usernamePwdToken = (UsernamePasswordAuthenticationToken) targetUser;
                        
                        OAuth2AccessToken accessToken = createImpersonationAccessToken(usernamePwdToken);
                        LOGGER.debug("!!! Impersonated access token is: " +  accessToken.getValue() );
                        //Set the cookies
                        OAuth2Properties  oauth2Properties = new OAuth2Properties();
                        OAuth2CookieHelper cookieHelper = new OAuth2CookieHelper(oauth2Properties);
                        OAuth2Cookies cookies = new OAuth2Cookies();
                        //TODO How to get remember me instead of hardcoding to true
                        cookieHelper.createCookies(request, accessToken, true, cookies);
                        cookies.addCookiesTo(response);
                        
                        // redirect to target url
                        this.successHandler.onAuthenticationSuccess(request, response,
                                        targetUser);
                }
                catch (AuthenticationException e) {
                        this.logger.debug("Switch User failed", e);
                        this.failureHandler.onAuthenticationFailure(request, response, e);
                }

                return;
        }
        else if (requiresExitUser(request)) {
                // get the original authentication object (if exists)
                Authentication originalUser = attemptExitUser(request);

                // update the current context back to the original user
                SecurityContextHolder.getContext().setAuthentication(originalUser);

                // redirect to target url
                this.successHandler.onAuthenticationSuccess(request, response, originalUser);

                return;
        }

        chain.doFilter(request, response);
          //put the 
        LOGGER.debug("!!!! END doFilter MSSwitchUserFilter");
    }
    
    
    @Override
    protected Authentication attemptSwitchUser(HttpServletRequest request) throws AuthenticationException {
        
            return switchUser(request);
    }

    

    private Authentication switchUser(HttpServletRequest request) {
        String username = request.getParameter(SPRING_SECURITY_SWITCH_USERNAME_KEY);
        
        if (username == null) {
            username = "";
        }
        LOGGER.debug("!!!!! username from request: " + username );
        LOGGER.debug("!!!!! Attempt to switch to user [" + username + "]");

        UserDetails targetUser = userDetailsService.loadUserByUsername(username);

        // OK, create the switch user token
        UsernamePasswordAuthenticationToken targetUserRequest = createSwitchUserToken(request, targetUser);

        LOGGER.debug("!!!!!Switch User Token [" + targetUserRequest + "]");
        //SecurityContextHolder.getContext().setAuthentication(targetUserRequest);
         return targetUserRequest;
    }
    
    
    private UsernamePasswordAuthenticationToken createSwitchUserToken(HttpServletRequest request, UserDetails targetUser) {
        // grant an additional authority that contains the original
        // Authentication object
        // which will be used to 'exit' from the current switched user.

        /* TODO Cami uncomment this part after testing context switching .... 
         
        Authentication currentAuth;
        
        try {
            // SEC-1763. Check first if we are already switched.
            currentAuth = attemptExitUser(request);
        } catch (AuthenticationCredentialsNotFoundException e) {
            currentAuth = SecurityContextHolder.getContext().getAuthentication();
        }*/
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        GrantedAuthority switchAuthority = new SwitchUserGrantedAuthority(ROLE_PREVIOUS_ADMINISTRATOR, currentAuth);

        // get the original authorities
        Collection<? extends GrantedAuthority> orig = targetUser.getAuthorities();

        // add the new switch user authority
        List<GrantedAuthority> newAuths = new ArrayList<GrantedAuthority>(orig);
        newAuths.add(switchAuthority);

        // create the new authentication token
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(targetUser.getUsername(), null, newAuths);
        authentication.setDetails(targetUser);
        
        return authentication;
    }
    
    /**
     * Sets the URL to go to after a successful switch / exit user request. Use
     * {@link #setSuccessHandler(AuthenticationSuccessHandler) setSuccessHandler} instead
     * if you need more customized behaviour.
     *
     * @param targetUrl The target url.
     */
    public void setTargetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
    }

    /**
     * Used to define custom behaviour on a successful switch or exit user.
     * <p>
     * Can be used instead of setting <tt>targetUrl</tt>.
     */
    public void setSuccessHandler(AuthenticationSuccessHandler successHandler) {
            Assert.notNull(successHandler, "successHandler cannot be null");
            this.successHandler = successHandler;
    }
    
    
    private OAuth2AccessToken createImpersonationAccessToken(UsernamePasswordAuthenticationToken userPasswordAuthentiation) {
        LOGGER.debug("!!!! Create impersonation access token:");
        Map<String, String> parameters = new HashMap<>();   
        UaaProperties uaaProperties = new UaaProperties();
        ClientDetails client = clientDetailsService.loadClientByClientId(uaaProperties.getWebClientConfiguration().getClientId());
        OAuth2Request oauthRequest = new OAuth2Request(parameters, client.getClientId(), 
                client.getAuthorities(), true, 
                client.getScope(), client.getResourceIds(), null, null, null);
        OAuth2Authentication authentication = new OAuth2Authentication(oauthRequest, userPasswordAuthentiation);
        OAuth2AccessToken createAccessToken = tokenService.createAccessToken(authentication);
        return createAccessToken;
    }

}
