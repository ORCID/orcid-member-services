package org.orcid.mp.user.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orcid.mp.user.UserServiceApplication;
import org.orcid.mp.user.client.MemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.repository.UserRepository;
import org.orcid.mp.user.rest.error.SimpleExceptionHandler;
import org.orcid.mp.user.rest.validation.UserValidator;
import org.orcid.mp.user.rest.vm.ManagedUserVM;
import org.orcid.mp.user.security.AuthoritiesConstants;
import org.orcid.mp.user.service.AuthorityService;
import org.orcid.mp.user.service.MailService;
import org.orcid.mp.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserServiceApplication.class)
public class UserResourceIT {

    private static final String DEFAULT_PASSWORD = "password";
    private static final String LOGGED_IN_PASSWORD = "0123456789";
    private static final String UPDATED_PASSWORD = "new-password";

    private static final String DEFAULT_EMAIL = "user@orcid.org";
    private static final String LOGGED_IN_EMAIL = "loggedin@orcid.org";

    private static final String DEFAULT_FIRSTNAME = "user";
    private static final String LOGGED_IN_FIRSTNAME = "logged";
    private static final String UPDATED_FIRSTNAME = "jhipsterFirstName";

    private static final String DEFAULT_LASTNAME = "last-name";
    private static final String LOGGED_IN_LASTNAME = "in";
    private static final String UPDATED_LASTNAME = "jhipsterLastName";

    private static final String DEFAULT_SALESFORCE_ID = "salesforceId";
    private static final String DEFAULT_MEMBER_NAME = "memberName";
    private static final String LOGGED_IN_SALESFORCE_ID = "salesforceId";
    private static final String LOGGED_IN_MEMBER_NAME = "memberName";
    private static final String UPDATED_SALESFORCE_ID = "anotherSalesforceId";
    private static final String UPDATED_MEMBER_NAME = "anotherMemberName";
    private static final String ADMIN_MEMBER_NAME = "admin-member-name";
    private static final String ADMIN_SALESFORCE_ID = "admin-member-id";

    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String UPDATED_IMAGEURL = "http://placehold.it/40x40";

    private static final String DEFAULT_LANGKEY = "en";
    private static final String UPDATED_LANGKEY = "fr";

    private static final String MAIN_CONTACT_EMAIL = "main.contact@orcid.org";

    private static final String MAIN_CONTACT_SALESFORCE_ID = "main-contact";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private UserResource userResource;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthorityService authorityService;

    private MockMvc restUserMockMvc;

    private User user;

    private ObjectMapper objectMapper;

    @Autowired
    private UserValidator userValidator;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        userRepository.deleteAll();
        user = createEntity();
        createLoggedInUser();
        createOrgOwnerUser();

        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource).setCustomArgumentResolvers(pageableArgumentResolver).setControllerAdvice(new SimpleExceptionHandler())
                .setMessageConverters(jacksonMessageConverter).build();

        // mock out calls to member service
        MemberServiceClient memberServiceClient = Mockito.mock(MemberServiceClient.class);
        when(memberServiceClient.getMember(eq(DEFAULT_SALESFORCE_ID))).thenReturn(getDefaultMember());
        when(memberServiceClient.getMember(eq(UPDATED_SALESFORCE_ID))).thenReturn(getUpdatedMember());
        when(memberServiceClient.getMember(eq(ADMIN_SALESFORCE_ID))).thenReturn(getAdminMember());

        ReflectionTestUtils.setField(userService, "memberServiceClient", memberServiceClient);
        ReflectionTestUtils.setField(userMapper, "memberServiceClient", memberServiceClient);
        ReflectionTestUtils.setField(authorityService, "memberServiceClient", memberServiceClient);
        ReflectionTestUtils.setField(userValidator, "memberServiceClient", memberServiceClient);
        ReflectionTestUtils.setField(userService, "mailService", Mockito.mock(MailService.class));
    }

    private Member getDefaultMember() {
        Member member = new Member();
        member.setClientName(DEFAULT_MEMBER_NAME);
        member.setSalesforceId(DEFAULT_SALESFORCE_ID);
        return member;
    }

    private Member getAdminMember() {
        Member member = new Member();
        member.setClientName(ADMIN_MEMBER_NAME);
        member.setSalesforceId(ADMIN_SALESFORCE_ID);
        member.setSuperadminEnabled(true);
        return member;
    }

    private Member getUpdatedMember() {
        Member member = new Member();
        member.setClientName(UPDATED_MEMBER_NAME);
        member.setSalesforceId(UPDATED_SALESFORCE_ID);
        return member;
    }

    private void createLoggedInUser() {
        User user = new User();
        user.setPassword(LOGGED_IN_PASSWORD);
        user.setActivated(true);
        user.setEmail(LOGGED_IN_EMAIL);
        user.setFirstName(LOGGED_IN_FIRSTNAME);
        user.setLastName(LOGGED_IN_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(LOGGED_IN_SALESFORCE_ID);
        user.setMemberName(LOGGED_IN_MEMBER_NAME);
        user.setMainContact(false);
        userRepository.save(user);
    }

    private void createOrgOwnerUser() {
        User user = new User();
        user.setPassword(LOGGED_IN_PASSWORD);
        user.setActivated(true);
        user.setEmail(MAIN_CONTACT_EMAIL);
        user.setFirstName(LOGGED_IN_FIRSTNAME);
        user.setLastName(LOGGED_IN_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(MAIN_CONTACT_SALESFORCE_ID);
        user.setMemberName(LOGGED_IN_MEMBER_NAME);
        user.setMainContact(true);
        userRepository.save(user);
    }

    private User createEntity() {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(DEFAULT_SALESFORCE_ID);
        user.setMainContact(false);
        return user;
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void createUser() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        // Create the User
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setMainContact(false);
        managedUserVM.setSalesforceId(DEFAULT_SALESFORCE_ID);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(post("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isCreated());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate + 1);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testUser.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(testUser.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(testUser.getSalesforceId()).isEqualTo(DEFAULT_SALESFORCE_ID);
        assertThat(testUser.getMemberName()).isEqualTo(DEFAULT_MEMBER_NAME);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void createUserWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId("1L");
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // An entity with an existing ID cannot be created, so this API call
        // must fail
        restUserMockMvc.perform(post("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void createUserWithExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Create the User
        restUserMockMvc.perform(post("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void createUserWithExistingEmail() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);// this email should already be
        // used
        managedUserVM.setMainContact(false);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Create the User
        restUserMockMvc.perform(post("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void createUserWithRoleAdmin_memberNotAdminEnabled() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        // Create the User
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setSalesforceId(DEFAULT_SALESFORCE_ID);
        managedUserVM.setMainContact(true);
        managedUserVM.setIsAdmin(true);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Mocked member does not have superadmin enabled so this request
        // must fail
        restUserMockMvc.perform(post("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void createUserWithRoleAdmin() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        // Create the User
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setPassword(DEFAULT_PASSWORD);
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(DEFAULT_LANGKEY);
        managedUserVM.setSalesforceId(ADMIN_SALESFORCE_ID);
        managedUserVM.setMainContact(true);
        managedUserVM.setIsAdmin(true);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Mocked member does not have superadmin enabled so this request
        // must fail
        restUserMockMvc.perform(post("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isCreated());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate + 1);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void getAllUsers() throws Exception {
        // Initialize the database
        userRepository.save(user);

        restUserMockMvc.perform(get("/users").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.content[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
                .andExpect(jsonPath("$.content[*].lastName").value(hasItem(DEFAULT_LASTNAME))).andExpect(jsonPath("$.content[*].email").value(hasItem(DEFAULT_EMAIL)))
                .andExpect(jsonPath("$.content[*].imageUrl").value(hasItem(DEFAULT_IMAGEURL))).andExpect(jsonPath("$.content[*].langKey").value(hasItem(DEFAULT_LANGKEY)));
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void getUser() throws Exception {
        // Initialize the database
        userRepository.save(user);

        // Get the user
        MvcResult result = restUserMockMvc.perform(get("/users/{login}", user.getEmail())).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME)).andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
                .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGEURL)).andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY)).andReturn();

        UserDTO read = objectMapper.readValue(result.getResponse().getContentAsString(), UserDTO.class);
        assertThat(read.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    public void getNonExistingUser() throws Exception {
        restUserMockMvc.perform(get("/users/unknown")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void updateUser() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        // Update the user
        User updatedUser = userRepository.findById(user.getId()).get();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(updatedUser.getId());
        managedUserVM.setPassword(UPDATED_PASSWORD);
        managedUserVM.setFirstName(UPDATED_FIRSTNAME);
        managedUserVM.setLastName(UPDATED_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(updatedUser.getActivated());
        managedUserVM.setImageUrl(UPDATED_IMAGEURL);
        managedUserVM.setLangKey(UPDATED_LANGKEY);
        managedUserVM.setMainContact(false);
        managedUserVM.setCreatedBy(updatedUser.getCreatedBy());
        managedUserVM.setCreatedDate(updatedUser.getCreatedDate());
        managedUserVM.setLastModifiedBy(updatedUser.getLastModifiedBy());
        managedUserVM.setLastModifiedDate(updatedUser.getLastModifiedDate());
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
        managedUserVM.setSalesforceId(UPDATED_SALESFORCE_ID);

        restUserMockMvc.perform(put("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isOk());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeUpdate);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testUser.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
        assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
        assertThat(testUser.getSalesforceId()).isEqualTo(UPDATED_SALESFORCE_ID);
        assertThat(testUser.getMemberName()).isEqualTo(UPDATED_MEMBER_NAME);
    }

    @Test
    @WithMockUser(username = MAIN_CONTACT_EMAIL, authorities = {"ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void getUsersBySalesforceId() throws Exception {
        saveTenUsersWithSalesforceId(MAIN_CONTACT_SALESFORCE_ID);
        userRepository.save(user);

        MvcResult result = restUserMockMvc
                .perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(11)).andReturn();

        restUserMockMvc
                .perform(get("/users/salesforce/incorrect-salesforce-id/p").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());

        result = restUserMockMvc.perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "firstname").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(10)).andReturn();

        result = restUserMockMvc.perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "firstname 1").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2)).andReturn();

        result = restUserMockMvc.perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "lastname 2").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "membername 3").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "4@" + MAIN_CONTACT_SALESFORCE_ID + ".org").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc
                .perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "%2B").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc
                .perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "10%2Btest").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc
                .perform(get("/users/salesforce/" + MAIN_CONTACT_SALESFORCE_ID + "/p").param("filter", "lastname+10%2Btest").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        // bad salesforce id
        restUserMockMvc.perform(get("/users/salesforce/salesforce-id-5/p").param("filter", "4@orcid.org").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_USER", "ROLE_ADMIN"}, password = LOGGED_IN_PASSWORD)
    public void getUsersBySalesforceId_notMainContact() throws Exception {
        MvcResult result = restUserMockMvc.perform(
                        get("/users/salesforce/" + LOGGED_IN_SALESFORCE_ID + "/p").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isUnauthorized()).andReturn();
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_USER", "ROLE_ADMIN"}, password = LOGGED_IN_PASSWORD)
    public void getAllUsersWithFilter() throws Exception {
        saveTenUsersWithSalesforceId("salesforce-id-1");
        saveTenUsersWithSalesforceId("salesforce-id-2");
        saveTenUsersWithSalesforceId("salesforce-id-3");
        userRepository.save(user);

        MvcResult result = restUserMockMvc.perform(
                        get("/users").param("filter", "firstname").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(30)).andReturn();

        result = restUserMockMvc
                .perform(get("/users").param("filter", "firstname 1").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(6)).andReturn();

        result = restUserMockMvc
                .perform(get("/users").param("filter", "lastname 2").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3)).andReturn();

        result = restUserMockMvc
                .perform(get("/users").param("filter", "membername 3").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3)).andReturn();

        result = restUserMockMvc
                .perform(get("/users").param("filter", "4@salesforce-id-1.org").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc
                .perform(get("/users").param("filter", "%2B").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3)).andReturn();

        result = restUserMockMvc
                .perform(get("/users").param("filter", "10%2Btest@salesforce-id-1.org").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc
                .perform(get("/users").param("filter", "lastname+10%2Btest").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3)).andReturn();
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void getAllUsersWithoutAdminRole() throws Exception {
        MvcResult result = restUserMockMvc.perform(
                        get("/users").param("filter", "firstname").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isForbidden()).andReturn();
    }

    private void saveTenUsersWithSalesforceId(String salesforceId) {
        for (int i = 0; i < 9; i++) {
            userRepository.save(createUser(String.valueOf(i), salesforceId));
        }
        // Add one user with a special character
        userRepository.save(createUser("10+test", salesforceId));
    }

    private User createUser(String id, String salesforceId) {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail(id + "@" + salesforceId + ".org");
        user.setFirstName("firstname " + id);
        user.setLastName("lastname " + id);
        user.setMemberName("membername " + id);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(salesforceId);
        user.setMainContact(false);
        return user;
    }


    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USR"}, password = LOGGED_IN_PASSWORD)
    public void updateUserWithRoleAdmin() throws Exception {
        // Initialize the database
        userRepository.save(user);

        // Update the user
        User updatedUser = userRepository.findById(user.getId()).get();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(updatedUser.getId());
        managedUserVM.setPassword(UPDATED_PASSWORD);
        managedUserVM.setFirstName(UPDATED_FIRSTNAME);
        managedUserVM.setLastName(UPDATED_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(updatedUser.getActivated());
        managedUserVM.setImageUrl(UPDATED_IMAGEURL);
        managedUserVM.setLangKey(UPDATED_LANGKEY);
        managedUserVM.setCreatedBy(updatedUser.getCreatedBy());
        managedUserVM.setCreatedDate(updatedUser.getCreatedDate());
        managedUserVM.setLastModifiedBy(updatedUser.getLastModifiedBy());
        managedUserVM.setLastModifiedDate(updatedUser.getLastModifiedDate());
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
        managedUserVM.setSalesforceId(ADMIN_SALESFORCE_ID);
        managedUserVM.setIsAdmin(true);

        restUserMockMvc.perform(put("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USR"}, password = LOGGED_IN_PASSWORD)
    public void updateUserWithRoleAdmin_memberNotAdminEnabled() throws Exception {
        // Initialize the database
        userRepository.save(user);

        // Update the user
        User updatedUser = userRepository.findById(user.getId()).get();

        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setId(updatedUser.getId());
        managedUserVM.setPassword(UPDATED_PASSWORD);
        managedUserVM.setFirstName(UPDATED_FIRSTNAME);
        managedUserVM.setLastName(UPDATED_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(updatedUser.getActivated());
        managedUserVM.setImageUrl(UPDATED_IMAGEURL);
        managedUserVM.setLangKey(UPDATED_LANGKEY);
        managedUserVM.setCreatedBy(updatedUser.getCreatedBy());
        managedUserVM.setCreatedDate(updatedUser.getCreatedDate());
        managedUserVM.setLastModifiedBy(updatedUser.getLastModifiedBy());
        managedUserVM.setLastModifiedDate(updatedUser.getLastModifiedDate());
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
        managedUserVM.setSalesforceId(DEFAULT_SALESFORCE_ID);
        managedUserVM.setIsAdmin(true);

        restUserMockMvc.perform(put("/users").contentType(RestTestUtil.APPLICATION_JSON_UTF8).content(RestTestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUserEquals() throws Exception {
        RestTestUtil.equalsVerifier(User.class);
        User user1 = new User();
        user1.setId("id1");
        User user2 = new User();
        user2.setId(user1.getId());
        assertThat(user1).isEqualTo(user2);
        user2.setId("id2");
        assertThat(user1).isNotEqualTo(user2);
        user1.setId(null);
        assertThat(user1).isNotEqualTo(user2);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract class PageMixin {
        // Empty class, used only for holding annotations
    }


}
