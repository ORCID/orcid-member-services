package org.orcid.user.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

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

import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;


public class MSSwitchUserFilter extends SwitchUserFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MSSwitchUserFilter.class);
    
    private final UserDetailsService userDetailsService;
    
    @Autowired
    private AuthorizationServerEndpointsConfiguration configuration;
    
    public MSSwitchUserFilter(UserDetailsService userDetailsService ) {
        this.userDetailsService = userDetailsService;
        
    }
    
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        LOGGER.debug("!!!!! inside MSSwitchUserFilter");
        
        super.doFilter(req, res, chain);
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
        SecurityContextHolder.getContext().setAuthentication(targetUserRequest);
        LOGGER.debug("!!! Get from context: " + SecurityContextHolder.getContext().getAuthentication().toString() );
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

}
