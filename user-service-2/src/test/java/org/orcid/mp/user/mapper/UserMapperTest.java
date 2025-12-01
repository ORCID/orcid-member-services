package org.orcid.mp.user.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.user.client.MemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.security.AuthoritiesConstants;
import org.orcid.mp.user.service.AuthorityService;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

public class UserMapperTest {

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private AuthorityService authorityService;

    @InjectMocks
    private UserMapper userMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(memberServiceClient.getMember(eq("salesforce1"))).thenReturn(getMember());
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

    private Member getMember() {
        Member member = new Member();
        member.setClientName("member 1");
        member.setSalesforceId("salesforce1");
        return member;
    }

}
