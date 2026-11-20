package org.orcid.mp.user.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.orcid.mp.user.UserServiceApplication;
import org.orcid.mp.user.client.InternalMemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserServiceApplication.class)
@AutoConfigureMockMvc
class InternalResourceIT {

    private static final String CREATED_BY = "member-service-admin@orcid.org";
    private static final String LAST_MODIFIED_BY = "another-admin@orcid.org";
    private static final String MEMBER_ID = "member-id";
    private static final String EMAIL = "main.contact@orcid.org";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InternalMemberServiceClient internalMemberServiceClient;

    @Test
    void createMainContactUser_passesCreatedByAndLastModifiedByToUserService() throws Exception {
        when(internalMemberServiceClient.getMember(MEMBER_ID)).thenReturn(new Member());
        when(userService.createUser(any(UserDTO.class), anyString())).thenReturn(new UserDTO());

        mockMvc.perform(post("/internal/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_internal")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildPayload())))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        ArgumentCaptor<UserDTO> userCaptor = ArgumentCaptor.forClass(UserDTO.class);
        ArgumentCaptor<String> createdByCaptor = ArgumentCaptor.forClass(String.class);
        verify(userService).createUser(userCaptor.capture(), createdByCaptor.capture());

        UserDTO captured = userCaptor.getValue();
        assertThat(captured.getCreatedBy()).isEqualTo(CREATED_BY);
        assertThat(captured.getLastModifiedBy()).isEqualTo(LAST_MODIFIED_BY);
        assertThat(captured.getEmail()).isEqualTo(EMAIL);
        assertThat(captured.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(captured.getMainContact()).isTrue();
        assertThat(createdByCaptor.getValue()).isEqualTo(CREATED_BY);
    }

    @Test
    void createMainContactUser_returns500WhenServiceFails() throws Exception {
        when(internalMemberServiceClient.getMember(MEMBER_ID)).thenReturn(new Member());
        when(userService.createUser(any(UserDTO.class), anyString())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/internal/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_internal")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildPayload())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createMainContactUser_withoutInternalScope_isRejected() throws Exception {
        mockMvc.perform(post("/internal/users")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildPayload())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    /**
     * Raw JSON payload, as an internal client (e.g. member service) would send it.
     */
    private Map<String, Object> buildPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", EMAIL);
        payload.put("firstName", "Main");
        payload.put("lastName", "Contact");
        payload.put("memberId", MEMBER_ID);
        payload.put("memberName", "Member Name");
        payload.put("mainContact", true);
        payload.put("langKey", "en");
        payload.put("createdBy", CREATED_BY);
        payload.put("lastModifiedBy", LAST_MODIFIED_BY);
        return payload;
    }
}
