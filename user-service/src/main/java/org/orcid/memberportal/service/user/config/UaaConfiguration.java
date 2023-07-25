package org.orcid.memberportal.service.user.config;

import java.net.MalformedURLException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.orcid.memberportal.service.user.security.PasswordTokenGranter;
import org.orcid.memberportal.service.user.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileUrlResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenGranter;
import org.springframework.security.oauth2.provider.refresh.RefreshTokenGranter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

import io.github.jhipster.config.JHipsterProperties;

@Configuration
@EnableAuthorizationServer
public class UaaConfiguration extends AuthorizationServerConfigurerAdapter implements ApplicationContextAware {
    /**
     * Access tokens will not expire any earlier than this.
     */
    private static final int MIN_ACCESS_TOKEN_VALIDITY_SECS = 60;

    private static final Logger LOG = LoggerFactory.getLogger(UaaConfiguration.class);

    private ApplicationContext applicationContext;

    @Autowired
    private UserService userService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @EnableResourceServer
    public static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        private final TokenStore tokenStore;

        private final JHipsterProperties jHipsterProperties;

        private final CorsFilter corsFilter;

        public ResourceServerConfiguration(TokenStore tokenStore, JHipsterProperties jHipsterProperties, CorsFilter corsFilter) {
            this.tokenStore = tokenStore;
            this.jHipsterProperties = jHipsterProperties;
            this.corsFilter = corsFilter;
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.exceptionHandling().authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)).and().csrf()
                .disable().addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class).headers().frameOptions().disable().and().sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().authorizeRequests().antMatchers("/api/register").permitAll()
                .antMatchers("/api/activate").permitAll().antMatchers("/api/authenticate").permitAll().antMatchers("/api/account/reset-password/init").permitAll()
                .antMatchers("/api/account/reset-password/finish").permitAll().antMatchers("/api/account/reset-password/validate").permitAll()
                .antMatchers("/api/users/**/resendActivation").permitAll().antMatchers("/api/**").authenticated().antMatchers("/management/health").permitAll()
                .antMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN).antMatchers("/v2/api-docs/**").permitAll()
                .antMatchers("/swagger-resources/configuration/ui").permitAll().antMatchers("/swagger-ui/index.html").hasAuthority(AuthoritiesConstants.ADMIN);
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId("jhipster-uaa").tokenStore(tokenStore);
        }
    }

    private final JHipsterProperties jHipsterProperties;

    private final UaaProperties uaaProperties;

    private final PasswordEncoder passwordEncoder;

    public UaaConfiguration(JHipsterProperties jHipsterProperties, UaaProperties uaaProperties, PasswordEncoder passwordEncoder) {
        this.jHipsterProperties = jHipsterProperties;
        this.uaaProperties = uaaProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        int accessTokenValidity = uaaProperties.getWebClientConfiguration().getAccessTokenValidityInSeconds();
        accessTokenValidity = Math.max(accessTokenValidity, MIN_ACCESS_TOKEN_VALIDITY_SECS);
        int refreshTokenValidity = uaaProperties.getWebClientConfiguration().getRefreshTokenValidityInSecondsForRememberMe();
        refreshTokenValidity = Math.max(refreshTokenValidity, accessTokenValidity);
        clients.inMemory().withClient(uaaProperties.getWebClientConfiguration().getClientId())
            .secret(passwordEncoder.encode(uaaProperties.getWebClientConfiguration().getSecret())).scopes("openid").autoApprove(true)
            .authorizedGrantTypes("implicit", "refresh_token", "password", "authorization_code").accessTokenValiditySeconds(accessTokenValidity)
            .refreshTokenValiditySeconds(refreshTokenValidity).and().withClient(jHipsterProperties.getSecurity().getClientAuthorization().getClientId())
            .secret(passwordEncoder.encode(jHipsterProperties.getSecurity().getClientAuthorization().getClientSecret())).scopes("web-app").authorities("ROLE_ADMIN")
            .autoApprove(true).authorizedGrantTypes("client_credentials")
            .accessTokenValiditySeconds((int) jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSeconds())
            .refreshTokenValiditySeconds((int) jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSecondsForRememberMe());
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        Collection<TokenEnhancer> tokenEnhancers = applicationContext.getBeansOfType(TokenEnhancer.class).values();
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(new ArrayList<>(tokenEnhancers));

        // don't reuse refresh tokens to avoid inactivity timeouts
        endpoints.authenticationManager(authenticationManager).tokenStore(tokenStore()).tokenEnhancer(tokenEnhancerChain).reuseRefreshTokens(false);
        endpoints.tokenGranter(tokenGranter(endpoints));
    }

    @Bean
    public DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setTokenEnhancer(jwtAccessTokenConverter());
        defaultTokenServices.setAccessTokenValiditySeconds((int) jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSeconds());
        defaultTokenServices.setRefreshTokenValiditySeconds((int) jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSecondsForRememberMe());
        return defaultTokenServices;
    }

    private TokenGranter tokenGranter(final AuthorizationServerEndpointsConfigurer endpoints) {
        PasswordTokenGranter passwordTokenGranter = new PasswordTokenGranter(endpoints, authenticationManager, userService, tokenServices());
        RefreshTokenGranter refreshTokenGranter = new RefreshTokenGranter(tokenServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
        ClientCredentialsTokenGranter clientCredentialsTokenGranter = new ClientCredentialsTokenGranter(tokenServices(), endpoints.getClientDetailsService(),
            endpoints.getOAuth2RequestFactory());
        return new CompositeTokenGranter(Arrays.asList(passwordTokenGranter, refreshTokenGranter, clientCredentialsTokenGranter));
    }

    @Autowired
    @Qualifier("authenticationManagerBean")
    private AuthenticationManager authenticationManager;

    /**
     * Apply the token converter (and enhancer) for token store.
     *
     * @return the {@link JwtTokenStore} managing the tokens.
     */
    @Bean
    public JwtTokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    /**
     * This bean generates an token enhancer, which manages the exchange between
     * JWT access tokens and Authentication in both directions.
     *
     * @return an access token converter configured with the authorization
     * server's public/private keys.
     */
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        try {
            KeyPair keyPair = new KeyStoreKeyFactory(new FileUrlResource(uaaProperties.getKeyStore().getName()),
                uaaProperties.getKeyStore().getPassword().toCharArray()).getKeyPair(uaaProperties.getKeyStore().getAlias());
            converter.setKeyPair(keyPair);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error creating keystore factory", e);
        }
        return converter;
    }

    public class RefreshTokenConverter extends JwtAccessTokenConverter {
        public RefreshTokenConverter() {
            super();
            try {
                KeyPair keyPair = new KeyStoreKeyFactory(new FileUrlResource(uaaProperties.getKeyStore().getName()),
                    uaaProperties.getKeyStore().getPassword().toCharArray()).getKeyPair(uaaProperties.getKeyStore().getAlias());
                super.setKeyPair(keyPair);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Error creating keystore factory", e);
            }
        }

        public Map<String, Object> decode(String token) {
            return super.decode(token);
        }
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()");
    }
}
