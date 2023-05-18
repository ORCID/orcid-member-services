package org.orcid.memberportal.service.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.dto.UserDTO;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.orcid.memberportal.service.user.services.AuthorityService;
import org.orcid.memberportal.service.user.services.MemberService;
import org.orcid.memberportal.service.user.services.UserService;

public class UserMapperTest {

    @Mock
    private MemberService memberService;

    @Mock
    private AuthorityService authorityService;

    @InjectMocks
    private UserMapper userMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(memberService.getMemberNameBySalesforce(Mockito.eq("salesforce1"))).thenReturn("member 1");
        Mockito.when(authorityService.getAuthoritiesForUser(Mockito.any(User.class))).thenReturn(new HashSet<String>(Arrays.asList(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER, AuthoritiesConstants.ORG_OWNER)));
    }

    @Test
    public void testToUser() {
        User user = userMapper.toUser(getUserDTO());
        assertThat(user.getFirstName()).isEqualTo("hello");
        assertThat(user.getLastName()).isEqualTo("orcid");
        assertThat(user.getEmail()).isEqualTo("hello@orcid.org");
        assertThat(user.getSalesforceId()).isEqualTo("salesforce1");
        assertThat(user.getMemberName()).isEqualTo("member 1");
        assertThat(user.getImageUrl()).isEqualTo("http://placehold.it/50x50");
        assertThat(user.getLangKey()).isEqualTo("en");
        assertThat(user.getAdmin()).isTrue();
    }

    @Test
    public void testToUserDTO() {
        UserDTO user = userMapper.toUserDTO(getUser());
        assertThat(user.getFirstName()).isEqualTo("first name");
        assertThat(user.getLastName()).isEqualTo("last name");
        assertThat(user.getEmail()).isEqualTo("some@email.com");
        assertThat(user.getSalesforceId()).isEqualTo("salesforce2");
        assertThat(user.getMemberName()).isEqualTo("member 2");
        assertThat(user.getImageUrl()).isEqualTo("http://placehold.it/50x50");
        assertThat(user.getLangKey()).isEqualTo("en");
        assertThat(user.getCreatedBy()).isEqualTo("someone");
        assertThat(user.getLastModifiedBy()).isEqualTo("some@email.com");
        assertThat(user.getIsAdmin()).isTrue();
        assertThat(user.getAuthorities()).isNotNull();
        assertThat(user.getAuthorities().size()).isEqualTo(3);
    }

    public UserDTO getUserDTO() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId("id");
        userDTO.setEmail("hello@orcid.org");
        userDTO.setFirstName("hello");
        userDTO.setLastName("orcid");
        userDTO.setSalesforceId("salesforce1");
        userDTO.setActivated(true);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey("en");
        userDTO.setCreatedBy("someone");
        userDTO.setLastModifiedBy("hello@orcid.org");
        userDTO.setIsAdmin(true);
        return userDTO;
    }

    public User getUser() {
        User user = new User();
        user.setId("id");
        user.setFirstName("first name");
        user.setLastName("last name");
        user.setEmail("some@email.com");
        user.setSalesforceId("salesforce2");
        user.setActivated(true);
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setCreatedBy("someone");
        user.setLastModifiedBy("some@email.com");
        user.setMemberName("member 2");
        user.setAdmin(true);
        user.setMainContact(true);
        return user;
    }

}
