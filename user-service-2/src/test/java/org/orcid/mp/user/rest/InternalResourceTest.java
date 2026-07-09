package org.orcid.mp.user.rest;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class InternalResourceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private InternalResource internalResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn("some@email.com");


        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testGetUserByLoginOrId_login() {
        when(userService.getUserByLogin(Mockito.eq("some@email.com"))).thenReturn(getUser());
        when(userMapper.toUserDTO(Mockito.any())).thenReturn(new UserDTO());

        ResponseEntity<UserDTO> response = internalResource.getUserByLoginOrId("some@email.com");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    void testGetUserByLoginOrId_notFound() {
        when(userService.getUserByLogin(Mockito.eq("some@email.com"))).thenReturn(Optional.empty());
        when(userService.getUser(Mockito.eq("some@email.com"))).thenReturn(Optional.empty());

        ResponseEntity<UserDTO> response = internalResource.getUserByLoginOrId("some@email.com");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetUserByLoginOrId_id() {
        when(userService.getUserByLogin(Mockito.eq("some@email.com"))).thenReturn(Optional.empty());
        when(userService.getUser(Mockito.eq("some@email.com"))).thenReturn(getUser());
        when(userMapper.toUserDTO(Mockito.any())).thenReturn(new UserDTO());

        ResponseEntity<UserDTO> response = internalResource.getUserByLoginOrId("some@email.com");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testUpdateUsersMemberName() {
        when(userService.updateUsersMemberName(Mockito.eq("member-id"), Mockito.eq("newName"))).thenReturn(true);
        ResponseEntity<Void> response = internalResource.updateUsersMemberName("member-id", "newName");
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    private Optional<User> getUser() {
        User user = new User();
        user.setEmail("some@email.com");
        user.setLangKey("en");
        user.setMemberId("some-memberId");
        user.setMainContact(true);
        return Optional.of(user);
    }
}
