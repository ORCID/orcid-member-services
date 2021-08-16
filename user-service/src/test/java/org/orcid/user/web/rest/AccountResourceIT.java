package org.orcid.user.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.UserServiceApp;
import org.orcid.user.config.Constants;
import org.orcid.user.domain.Authority;
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.service.MailService;
import org.orcid.user.service.MemberService;
import org.orcid.user.service.UserService;
import org.orcid.user.service.dto.PasswordChangeDTO;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.service.mapper.UserMapper;
import org.orcid.user.web.rest.errors.ExceptionTranslator;
import org.orcid.user.web.rest.vm.KeyAndPasswordVM;
import org.orcid.user.web.rest.vm.KeyVM;
import org.orcid.user.web.rest.vm.ManagedUserVM;
import org.orcid.user.web.rest.vm.PasswordResetResultVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AccountResource} REST controller.
 */
@SpringBootTest(classes = UserServiceApp.class)
public class AccountResourceIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HttpMessageConverter<?>[] httpMessageConverters;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    @Autowired
    private UserMapper userMapper;

    @Mock
    private MemberService mockMemberService;

    private MockMvc restMvc;

    private MockMvc restUserMockMvc;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail(any());

        Mockito.when(mockMemberService.memberExistsWithSalesforceId(Mockito.anyString())).thenReturn(Boolean.TRUE);
        Mockito.when(mockMemberService.getMemberNameBySalesforce(Mockito.anyString())).thenReturn("member");
        ReflectionTestUtils.setField(userMapper, "memberService", mockMemberService);

        AccountResource accountResource = new AccountResource(userRepository, userService, mockMailService, userMapper);

        AccountResource accountUserMockResource = new AccountResource(userRepository, mockUserService, mockMailService, userMapper);
        this.restMvc = MockMvcBuilders.standaloneSetup(accountResource).setMessageConverters(httpMessageConverters).setControllerAdvice(exceptionTranslator).build();
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource).setControllerAdvice(exceptionTranslator).build();
    }

    @Test
    public void testNonAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().string(""));
    }

    @Test
    public void testAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate").with(request -> {
            request.setRemoteUser("test");
            return request;
        }).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().string("test"));
    }

    @Test
    public void testGetExistingAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);

        User user = new User();
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("john.doe@jhipster.com");
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setAuthorities(authorities.stream().map(a -> a.getName()).collect(Collectors.toSet()));
        when(mockUserService.getUserWithAuthorities()).thenReturn(Optional.of(user));

        restUserMockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andExpect(jsonPath("$.firstName").value("john"))
                .andExpect(jsonPath("$.lastName").value("doe")).andExpect(jsonPath("$.email").value("john.doe@jhipster.com"))
                .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50")).andExpect(jsonPath("$.langKey").value("en"))
                .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
    }

    @Test
    public void testGetUnknownAccount() throws Exception {
        when(mockUserService.getUserWithAuthorities()).thenReturn(Optional.empty());

        restUserMockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_PROBLEM_JSON)).andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser("save-account@example.com")
    public void testSaveAccount() throws Exception {
        User user = new User();
        user.setEmail("save-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.save(user);

        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-account@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(post("/api/account").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(userDTO)))
                .andExpect(status().is2xxSuccessful());

        User updatedUser = userRepository.findOneByEmailIgnoreCase(user.getEmail()).orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @WithMockUser("save-invalid-email@example.com")
    public void testSaveInvalidEmail() throws Exception {
        User user = new User();
        user.setEmail("save-invalid-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.save(user);

        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("invalid email");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(post("/api/account").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(userDTO)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByEmailIgnoreCase("invalid email")).isNotPresent();
    }

    @Test
    @WithMockUser("save-existing-email@example.com")
    public void testSaveExistingEmail() throws Exception {
        User user = new User();
        user.setEmail("save-existing-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.save(user);

        User anotherUser = new User();
        anotherUser.setEmail("save-existing-email2@example.com");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);

        userRepository.save(anotherUser);

        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email2@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(post("/api/account").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(userDTO)))
                .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("save-existing-email@example.com").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email@example.com");
    }

    @Test
    @WithMockUser("save-existing-email@example.com")
    public void testSaveExistingEmailAndLogin() throws Exception {
        User user = new User();
        user.setEmail("save-existing-email-and-login@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.save(user);

        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email-and-login@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(post("/api/account").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(userDTO)))
                .andExpect(status().is4xxClientError());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("save-existing-email-and-login@example.com").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-and-login@example.com");
    }

    @Test
    @WithMockUser("change-password-wrong-existing-password@example.com")
    public void testChangePasswordWrongExistingPassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-wrong-existing-password@example.com");
        userRepository.save(user);

        restMvc.perform(post("/api/account/change-password").contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO("1" + currentPassword, "new password")))).andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-wrong-existing-password@example.com").orElse(null);
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isFalse();
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.getPassword())).isTrue();
    }

    @Test
    @WithMockUser("change-password@example.com")
    public void testChangePassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password@example.com");
        userRepository.save(user);

        restMvc.perform(post("/api/account/change-password").contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "new password")))).andExpect(status().isOk());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password@example.com").orElse(null);
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isTrue();
    }

    @Test
    @WithMockUser("change-password-too-small@example.com")
    public void testChangePasswordTooSmall() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-too-small@example.com");
        userRepository.save(user);

        String newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MIN_LENGTH - 1);

        restMvc.perform(post("/api/account/change-password").contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))).andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-too-small@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @WithMockUser("change-password-too-long@example.com")
    public void testChangePasswordTooLong() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-too-long@example.com");
        userRepository.save(user);

        String newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MAX_LENGTH + 1);

        restMvc.perform(post("/api/account/change-password").contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))).andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-too-long@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @WithMockUser("change-password-empty@example.com")
    public void testChangePasswordEmpty() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setEmail("change-password-empty@example.com");
        userRepository.save(user);

        restMvc.perform(post("/api/account/change-password").contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "")))).andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase("change-password-empty@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    public void testRequestPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail("password-reset@example.com");
        userRepository.save(user);

        restMvc.perform(post("/api/account/reset-password/init").content("password-reset@example.com")).andExpect(status().isOk());
    }

    @Test
    public void testRequestPasswordResetUpperCaseEmail() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail("password-reset@example.com");
        userRepository.save(user);

        restMvc.perform(post("/api/account/reset-password/init").content("password-reset@EXAMPLE.COM")).andExpect(status().isOk());
    }

    @Test
    public void testRequestPasswordResetWrongEmail() throws Exception {
        restMvc.perform(post("/api/account/reset-password/init").content("password-reset-wrong-email@example.com")).andExpect(status().isBadRequest());
    }

    @Test
    public void testFinishPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setEmail("finish-password-reset@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key");
        userRepository.save(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("new password");

        MvcResult result = restMvc
                .perform(
                        post("/api/account/reset-password/finish").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
                .andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        PasswordResetResultVM resetResult = mapper.readValue(result.getResponse().getContentAsByteArray(), PasswordResetResultVM.class);
        assertThat(resetResult.isSuccess()).isTrue();
        assertThat(resetResult.isInvalidKey()).isFalse();
        assertThat(resetResult.isExpiredKey()).isFalse();

        User updatedUser = userRepository.findOneByEmailIgnoreCase(user.getEmail()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();
    }

    @Test
    public void testFinishPasswordResetTooSmall() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setEmail("finish-password-reset-too-small@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key too small");
        userRepository.save(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("foo");

        restMvc.perform(post("/api/account/reset-password/finish").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
                .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByEmailIgnoreCase(user.getEmail()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isFalse();
    }

    @Test
    public void testFinishPasswordResetWrongKey() throws Exception {
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("wrong reset key");
        keyAndPassword.setNewPassword("new password");

        MvcResult result = restMvc
                .perform(
                        post("/api/account/reset-password/finish").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
                .andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        PasswordResetResultVM resetResult = mapper.readValue(result.getResponse().getContentAsByteArray(), PasswordResetResultVM.class);
        assertThat(resetResult.isSuccess()).isFalse();
        assertThat(resetResult.isInvalidKey()).isTrue();
        assertThat(resetResult.isExpiredKey()).isFalse(); // because key doesn't
                                                          // exist
    }

    @Test
    public void testFinishPasswordResetExpiredKey() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setEmail("finish-password-reset-expired@example.com");
        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS + 5000));
        user.setResetKey("resetkey");
        userRepository.save(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("resetkey");
        keyAndPassword.setNewPassword("something");

        MvcResult result = restMvc
                .perform(
                        post("/api/account/reset-password/finish").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
                .andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        PasswordResetResultVM resetResult = mapper.readValue(result.getResponse().getContentAsByteArray(), PasswordResetResultVM.class);
        assertThat(resetResult.isSuccess()).isFalse();
        assertThat(resetResult.isInvalidKey()).isFalse();
        assertThat(resetResult.isExpiredKey()).isTrue();
    }

    @Test
    public void testValidateKey_expiredKey() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setEmail("finish-password-reset-expired@example.com");
        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS + 5000));
        user.setResetKey("resetkey");
        userRepository.save(user);

        KeyVM key = new KeyVM();
        key.setKey("resetkey");

        MvcResult result = restMvc
                .perform(post("/api/account/reset-password/validate").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(key)))
                .andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        PasswordResetResultVM resetResult = mapper.readValue(result.getResponse().getContentAsByteArray(), PasswordResetResultVM.class);
        assertThat(resetResult.isSuccess()).isFalse();
        assertThat(resetResult.isInvalidKey()).isFalse();
        assertThat(resetResult.isExpiredKey()).isTrue();
    }

    @Test
    public void testValidateKey_invalidKey() throws Exception {
        KeyVM key = new KeyVM();
        key.setKey("incorrectValue");

        MvcResult result = restMvc
                .perform(post("/api/account/reset-password/validate").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(key)))
                .andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        PasswordResetResultVM resetResult = mapper.readValue(result.getResponse().getContentAsByteArray(), PasswordResetResultVM.class);
        assertThat(resetResult.isSuccess()).isFalse();
        assertThat(resetResult.isInvalidKey()).isTrue();
        assertThat(resetResult.isExpiredKey()).isFalse();
    }
}