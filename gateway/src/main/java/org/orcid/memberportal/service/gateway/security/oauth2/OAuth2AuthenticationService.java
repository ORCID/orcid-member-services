package org.orcid.memberportal.service.gateway.security.oauth2;

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.client.HttpClientErrorException;

import io.github.jhipster.security.PersistentTokenCache;

/**
 * Manages authentication cases for OAuth2 updating the cookies holding access
 * and refresh tokens accordingly.
 * <p>
 * It can authenticate users, refresh the token cookies should they expire and
 * log users out.
 */
public class OAuth2AuthenticationService {

    private final Logger LOG = LoggerFactory.getLogger(OAuth2AuthenticationService.class);

    /**
     * Number of milliseconds to cache refresh token grants so we don't have to
     * repeat them in case of parallel requests.
     */
    private static final long REFRESH_TOKEN_VALIDITY_MILLIS = 10000l;

    /**
     * Used to contact the OAuth2 token endpoint.
     */
    private final OAuth2TokenEndpointClient authorizationClient;

    /**
     * Helps us with cookie handling.
     */
    private final OAuth2CookieHelper cookieHelper;

    /**
     * Caches Refresh grant results for a refresh token value so we can reuse
     * them. This avoids hammering UAA in case of several multi-threaded
     * requests arriving in parallel.
     */
    private final PersistentTokenCache<OAuth2Cookies> recentlyRefreshed;

    private TokenStore tokenStore;

    public OAuth2AuthenticationService(OAuth2TokenEndpointClient authorizationClient, OAuth2CookieHelper cookieHelper, TokenStore tokenStore) {
        this.authorizationClient = authorizationClient;
        this.cookieHelper = cookieHelper;
        this.tokenStore = tokenStore;
        recentlyRefreshed = new PersistentTokenCache<>(REFRESH_TOKEN_VALIDITY_MILLIS);
    }

    /**
     * Authenticate the user by username and password.
     *
     * @param request
     *            the request coming from the client.
     * @param response
     *            the response going back to the server.
     * @param params
     *            the params holding the username, password, rememberMe, and optional mfaCode.
     * @return the LoginResult as a {@link ResponseEntity}. Will return
     *         {@code OK (200)}, if successful. If the UAA cannot authenticate
     *         the user, the status code returned by UAA will be returned.
     */
    public ResponseEntity<LoginResult> authenticate(HttpServletRequest request, HttpServletResponse response, Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            String mfaCode = params.get("mfaCode");
            boolean rememberMe = Boolean.valueOf(params.get("rememberMe"));
            OAuth2AccessToken accessToken = authorizationClient.sendPasswordGrant(username, password, mfaCode);

            LoginResult loginResult = getLoginResult(accessToken);
            
            if (!loginResult.isMfaRequired()) {
                LOG.info("Successfully authenticated user {}", params.get("username"));
                OAuth2Cookies cookies = new OAuth2Cookies();
                cookieHelper.createCookies(request, accessToken, rememberMe, cookies);
                cookies.addCookiesTo(response);
            }
            
            return ResponseEntity.ok(loginResult);
        } catch (HttpClientErrorException ex) {
            LOG.error("Failed to get OAuth2 tokens from UAA", ex);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    private LoginResult getLoginResult(OAuth2AccessToken accessToken) {
        OAuth2Authentication authentication = tokenStore.readAuthentication(accessToken);
        Collection<GrantedAuthority> authorities = authentication.getAuthorities();
        LoginResult loginResult = new LoginResult();
        authorities.forEach(a -> {
            if (a.getAuthority().equals("PRE_AUTH")) {
                loginResult.setMfaRequired(true);
            }
        });
        return loginResult;
    }

    /**
     * Try to refresh the access token using the refresh token provided as
     * cookie. Note that browsers typically send multiple requests in parallel
     * which means the access token will be expired on multiple threads. We
     * don't want to send multiple requests to UAA though, so we need to cache
     * results for a certain duration and synchronize threads to avoid sending
     * multiple requests in parallel.
     *
     * @param request
     *            the request potentially holding the refresh token.
     * @param response
     *            the response setting the new cookies (if refresh was
     *            successful).
     * @param refreshCookie
     *            the refresh token cookie. Must not be null.
     * @return the new servlet request containing the updated cookies for
     *         relaying downstream.
     */
    public HttpServletRequest refreshToken(HttpServletRequest request, HttpServletResponse response, Cookie refreshCookie) {
        // check if non-remember-me session has expired
        if (cookieHelper.isSessionExpired(refreshCookie)) {
            LOG.info("session has expired due to inactivity");
            logout(request, response); // logout to clear cookies in browser
            return stripTokens(request); // don't include cookies downstream
        }
        OAuth2Cookies cookies = getCachedCookies(refreshCookie.getValue());
        synchronized (cookies) {
            // check if we have a result from another thread already
            if (cookies.getAccessTokenCookie() == null) { // no, we are first!
                // send a refresh_token grant to UAA, getting new tokens
                String refreshCookieValue = OAuth2CookieHelper.getRefreshTokenValue(refreshCookie);
                OAuth2AccessToken accessToken = authorizationClient.sendRefreshGrant(refreshCookieValue);
                boolean rememberMe = OAuth2CookieHelper.isRememberMe(refreshCookie);
                cookieHelper.createCookies(request, accessToken, rememberMe, cookies);
                // add cookies to response to update browser
                cookies.addCookiesTo(response);
            } else {
                LOG.debug("reusing cached refresh_token grant");
            }
            // replace cookies in original request with new ones
            CookieCollection requestCookies = new CookieCollection(request.getCookies());
            requestCookies.add(cookies.getAccessTokenCookie());
            requestCookies.add(cookies.getRefreshTokenCookie());
            return new CookiesHttpServletRequestWrapper(request, requestCookies.toArray());
        }
    }

    /**
     * Get the result from the cache in a thread-safe manner.
     *
     * @param refreshTokenValue
     *            the refresh token for which we want the results.
     * @return a RefreshGrantResult for that token. This will either be empty,
     *         if we are the first one to do the request, or contain some
     *         results already, if another thread already handled the grant for
     *         us.
     */
    private OAuth2Cookies getCachedCookies(String refreshTokenValue) {
        synchronized (recentlyRefreshed) {
            OAuth2Cookies ctx = recentlyRefreshed.get(refreshTokenValue);
            if (ctx == null) {
                ctx = new OAuth2Cookies();
                recentlyRefreshed.put(refreshTokenValue, ctx);
            }
            return ctx;
        }
    }

    /**
     * Logs the user out by clearing all cookies.
     *
     * @param httpServletRequest
     *            the request containing the Cookies.
     * @param httpServletResponse
     *            the response used to clear them.
     */
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        cookieHelper.clearCookies(httpServletRequest, httpServletResponse);
    }

    /**
     * Strips token cookies preventing them from being used further down the
     * chain. For example, the OAuth2 client won't checked them and they won't
     * be relayed to other services.
     *
     * @param httpServletRequest
     *            the incoming request.
     * @return the request to replace it with which has the tokens stripped.
     */
    public HttpServletRequest stripTokens(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = cookieHelper.stripCookies(httpServletRequest.getCookies());
        return new CookiesHttpServletRequestWrapper(httpServletRequest, cookies);
    }
}
