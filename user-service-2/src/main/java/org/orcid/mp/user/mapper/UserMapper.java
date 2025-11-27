package org.orcid.mp.user.mapper;


import org.orcid.mp.user.client.MemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.service.AuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private MemberServiceClient memberServiceClient;

    @Autowired
    private AuthorityService authorityService;

    public User toUser(UserDTO userDTO) {
        Member member = memberServiceClient.getMember(userDTO.getSalesforceId());

        User user = new User();
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail().toLowerCase());
        user.setImageUrl(userDTO.getImageUrl());
        user.setDeleted(Boolean.FALSE);
        user.setSalesforceId(userDTO.getSalesforceId());
        user.setMemberName(member.getClientName());
        user.setMainContact(userDTO.getMainContact());
        user.setLangKey(userDTO.getLangKey());
        user.setActivated(userDTO.isActivated());
        user.setAdmin(userDTO.getIsAdmin());
        user.setMainContact(user.getMainContact());
        user.setId(userDTO.getId());
        return user;
    }

    public UserDTO toUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setEmail(user.getEmail());
        userDTO.setActivated(user.getActivated());
        userDTO.setImageUrl(user.getImageUrl());
        userDTO.setLangKey(user.getLangKey());
        userDTO.setCreatedBy(user.getCreatedBy());
        userDTO.setCreatedDate(user.getCreatedDate());
        userDTO.setLastModifiedBy(user.getLastModifiedBy());
        userDTO.setLastModifiedDate(user.getLastModifiedDate());
        userDTO.setAuthorities(authorityService.getAuthoritiesForUser(user));
        userDTO.setSalesforceId(user.getSalesforceId());
        userDTO.setMemberName(user.getMemberName());
        userDTO.setMainContact(user.getMainContact());
        userDTO.setId(user.getId());
        userDTO.setIsAdmin(user.getAdmin());
        userDTO.setMfaEnabled(user.getMfaEnabled() != null ? user.getMfaEnabled() : false);
        return userDTO;
    }

}
