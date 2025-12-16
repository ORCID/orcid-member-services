package org.orcid.mp.user.security;

import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.repository.UserRepository;
import org.orcid.mp.user.service.AuthorityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component("userDetailsService")
public class MemberPortalUserDetailsService implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(MemberPortalUserDetailsService.class);

    private final UserRepository userRepository;

    private final AuthorityService authorityService;

    public MemberPortalUserDetailsService(UserRepository userRepository, AuthorityService authorityService) {
        this.userRepository = userRepository;
        this.authorityService = authorityService;
    }

    @Override
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);

        if (new EmailValidator().isValid(login, null)) {
            return userRepository.findOneByEmailIgnoreCase(login).map(user -> createSpringSecurityUser(login, user))
                    .orElseThrow(() -> new UsernameNotFoundException("User with email " + login + " was not found in the database"));
        }

        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        return userRepository.findOneByEmailIgnoreCase(lowercaseLogin).map(user -> createSpringSecurityUser(lowercaseLogin, user))
                .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));

    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
        if (!user.getActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), getGrantedAuthorities(user));
    }

    private List<GrantedAuthority> getGrantedAuthorities(User user) {
        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        return authorities.stream().map(s -> new SimpleGrantedAuthority(s)).collect(Collectors.toList());
    }

}
