package org.orcid.mp.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.user.client.InternalMemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.service.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaAuthenticationProviderTest {

    @Mock private UserService userService;
    @Mock private InternalMemberServiceClient memberServiceClient;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private MfaAuthenticationProvider provider;

    private final String username = "test@example.com";
    private final String rawPassword = "password123";
    private final String memberId = "member-123";
    private final String validMfaCode = "123456";

    private UsernamePasswordAuthenticationToken authentication;
    private UserDetails mockUserDetails;
    private User mockStoredUser;
    private Member mockMember;

    @BeforeEach
    void setUp() {
        // Basic auth token setup (no MFA details by default)
        authentication = new UsernamePasswordAuthenticationToken(username, rawPassword);

        // Mock UserDetails (from UserDetailsService)
        mockUserDetails = new org.springframework.security.core.userdetails.User(
                username, "encodedPassword", Collections.emptyList()
        );

        // Mock Domain User (from UserService)
        mockStoredUser = new User();
        mockStoredUser.setEmail(username);
        mockStoredUser.setMemberId(memberId);

        // Mock Member (from MemberServiceClient)
        mockMember = new Member();
        mockMember.setActive(true);
    }

    @Test
    void authenticate_Success_NoMfaRequired() {
        // Given
        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
        when(passwordEncoder.matches(rawPassword, mockUserDetails.getPassword())).thenReturn(true);
        when(userService.getUserByLogin(username)).thenReturn(Optional.of(mockStoredUser));
        when(memberServiceClient.getMember(memberId)).thenReturn(mockMember);
        when(userService.isMfaEnabled(username)).thenReturn(false);

        // When
        Authentication result = provider.authenticate(authentication);

        // Then
        assertNotNull(result);
        assertEquals(username, ((UserDetails) result.getPrincipal()).getUsername());
        assertTrue(result.isAuthenticated());
    }

    @Test
    void authenticate_BadPassword_ThrowsBadCredentialsException() {
        // Given
        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
        when(passwordEncoder.matches(rawPassword, mockUserDetails.getPassword())).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(authentication));

        // Ensure we NEVER call the database if the password is wrong (prevents enumeration)
        verify(userService, never()).getUserByLogin(anyString());
        verify(memberServiceClient, never()).getMember(anyString());
    }

    @Test
    void authenticate_DeactivatedMember_ThrowsDeactivatedMemberException() {
        // Given
        mockMember.setActive(false); // Member is deactivated

        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
        when(passwordEncoder.matches(rawPassword, mockUserDetails.getPassword())).thenReturn(true);
        when(userService.getUserByLogin(username)).thenReturn(Optional.of(mockStoredUser));
        when(memberServiceClient.getMember(memberId)).thenReturn(mockMember);

        // When & Then
        assertThrows(DeactivatedMemberException.class, () -> provider.authenticate(authentication));
    }

    @Test
    void authenticate_MfaEnabled_MissingCode_ThrowsMfaRequiredException() {
        // Given
        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
        when(passwordEncoder.matches(rawPassword, mockUserDetails.getPassword())).thenReturn(true);
        when(userService.getUserByLogin(username)).thenReturn(Optional.of(mockStoredUser));
        when(memberServiceClient.getMember(memberId)).thenReturn(mockMember);
        when(userService.isMfaEnabled(username)).thenReturn(true);

        // When & Then (Token has no MFA details attached, so code is blank)
        assertThrows(MfaRequiredException.class, () -> provider.authenticate(authentication));
    }

    @Test
    void authenticate_MfaEnabled_InvalidCode_ThrowsMfaInvalidCodeException() {
        // Given
        MfaWebAuthenticationDetails mfaDetails = mock(MfaWebAuthenticationDetails.class);
        when(mfaDetails.getMfaCode()).thenReturn("wrong-code");
        authentication.setDetails(mfaDetails);

        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
        when(passwordEncoder.matches(rawPassword, mockUserDetails.getPassword())).thenReturn(true);
        when(userService.getUserByLogin(username)).thenReturn(Optional.of(mockStoredUser));
        when(memberServiceClient.getMember(memberId)).thenReturn(mockMember);
        when(userService.isMfaEnabled(username)).thenReturn(true);
        when(userService.validMfaCode(username, "wrong-code")).thenReturn(false);

        // When & Then
        assertThrows(MfaInvalidCodeException.class, () -> provider.authenticate(authentication));
    }

    @Test
    void authenticate_MfaEnabled_ValidCode_Success() {
        // Given
        MfaWebAuthenticationDetails mfaDetails = mock(MfaWebAuthenticationDetails.class);
        when(mfaDetails.getMfaCode()).thenReturn(validMfaCode);
        authentication.setDetails(mfaDetails);

        when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
        when(passwordEncoder.matches(rawPassword, mockUserDetails.getPassword())).thenReturn(true);
        when(userService.getUserByLogin(username)).thenReturn(Optional.of(mockStoredUser));
        when(memberServiceClient.getMember(memberId)).thenReturn(mockMember);
        when(userService.isMfaEnabled(username)).thenReturn(true);
        when(userService.validMfaCode(username, validMfaCode)).thenReturn(true);

        // When
        Authentication result = provider.authenticate(authentication);

        // Then
        assertNotNull(result);
        assertEquals(username, ((UserDetails) result.getPrincipal()).getUsername());
        assertTrue(result.isAuthenticated());
    }
}