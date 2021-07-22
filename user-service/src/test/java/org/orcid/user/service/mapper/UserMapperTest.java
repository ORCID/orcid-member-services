package org.orcid.user.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.domain.User;
import org.orcid.user.service.MemberService;
import org.orcid.user.service.dto.UserDTO;

public class UserMapperTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private UserMapper userMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(memberService.memberNameBySalesforce(Mockito.eq("salesforce1"))).thenReturn("member 1");
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
    }
    
    public UserDTO getUserDTO() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId("id");
        userDTO.setEmail("hello@orcid.org");
        userDTO.setFirstName("hello");
        userDTO.setLastName("orcid");
        userDTO.setSalesforceId("salesforce1");
        userDTO.setLogin("hello@orcid.org");
        userDTO.setActivated(true);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey("en");
        userDTO.setCreatedBy("someone");
        userDTO.setLastModifiedBy("hello@orcid.org");
        return userDTO;
    }
    
    public User getUser() {
        User user = new User();
        user.setId("id");
        user.setFirstName("first name");
        user.setLastName("last name");
        user.setEmail("some@email.com");
        user.setSalesforceId("salesforce2");
        user.setLogin("some@email.com");
        user.setActivated(true);
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setCreatedBy("someone");
        user.setLastModifiedBy("some@email.com");
        user.setMemberName("member 2");
        return user;
    }

}
