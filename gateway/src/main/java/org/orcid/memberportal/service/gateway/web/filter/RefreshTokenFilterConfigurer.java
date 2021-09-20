package org.orcid.memberportal.service.gateway.web.filter;

import org.orcid.memberportal.service.gateway.security.oauth2.OAuth2AuthenticationService;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.DefaultSecurityFilterChain;

/**
 * Configures a {@link RefreshTokenFilter} to refresh access tokens if they are
 * about to expire.
 *
 * @see RefreshTokenFilter
 */
public class RefreshTokenFilterConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
    /**
     * {@link RefreshTokenFilter} needs the {@link OAuth2AuthenticationService}
     * to refresh cookies using the refresh token.
     */
    private OAuth2AuthenticationService authenticationService;
    private final TokenStore tokenStore;

    public RefreshTokenFilterConfigurer(OAuth2AuthenticationService authenticationService, TokenStore tokenStore) {
        this.authenticationService = authenticationService;
        this.tokenStore = tokenStore;
    }

    /**
     * Install {@link RefreshTokenFilter} as a servlet Filter.
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        RefreshTokenFilter customFilter = new RefreshTokenFilter(authenticationService, tokenStore);
        http.addFilterBefore(customFilter, OAuth2AuthenticationProcessingFilter.class);
    }
}
