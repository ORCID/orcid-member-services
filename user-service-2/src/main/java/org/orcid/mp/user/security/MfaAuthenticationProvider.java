package org.orcid.mp.user.security;

import org.orcid.mp.user.client.InternalMemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class MfaAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MfaAuthenticationProvider.class);

    private final UserService userService;
    private final InternalMemberServiceClient memberServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public MfaAuthenticationProvider(UserService userService, PasswordEncoder passwordEncoder, UserDetailsService userDetailsService, InternalMemberServiceClient memberServiceClient) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.memberServiceClient = memberServiceClient;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        String mfaCode = "";
        if (authentication.getDetails() instanceof MfaWebAuthenticationDetails details) {
            mfaCode = details.getMfaCode();
        }

        UserDetails user = userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User storedUser = userService.getUserByLogin(username).orElseThrow();
        Member member = memberServiceClient.getMember(storedUser.getMemberId());
        if (!member.isActive()) {
            LOG.info("User {} login failed due to deactivated member", username);
            throw new DeactivatedMemberException();
        }

        if (userService.isMfaEnabled(username)) {
            if (mfaCode == null || mfaCode.isBlank()) {
                throw new MfaRequiredException("MFA_REQUIRED");
            }
            if (!userService.validMfaCode(username, mfaCode)) {
                throw new MfaInvalidCodeException("MFA_INVALID_CODE");
            }
        }

        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
