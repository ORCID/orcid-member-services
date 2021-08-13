package org.orcid.user.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orcid.user.UserServiceApp;
import org.orcid.user.domain.Authority;
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.service.MailService;
import org.orcid.user.service.MemberService;
import org.orcid.user.service.UserService;
import org.orcid.user.service.cache.UserCaches;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.service.mapper.UserMapper;
import org.orcid.user.web.rest.errors.ExceptionTranslator;
import org.orcid.user.web.rest.vm.ManagedUserVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Integration tests for the {@link UserResource} REST controller.
 */
@SpringBootTest(classes = UserServiceApp.class)
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

    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String UPDATED_IMAGEURL = "http://placehold.it/40x40";

    private static final String DEFAULT_LANGKEY = "en";
    private static final String UPDATED_LANGKEY = "fr";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserResource userResource;

    @Autowired
    private UserMapper userMapper;

    private MockMvc restUserMockMvc;

    private User user;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).clear();
        userRepository.deleteAll();
        user = createEntity();
        createLoggedInUser();

        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource).setCustomArgumentResolvers(pageableArgumentResolver).setControllerAdvice(exceptionTranslator)
                .setMessageConverters(jacksonMessageConverter).build();

        // mock out calls to member service
        MemberService mockedMemberService = Mockito.mock(MemberService.class);
        Mockito.when(mockedMemberService.memberExistsWithSalesforceId(Mockito.anyString())).thenReturn(Boolean.TRUE);
        Mockito.when(mockedMemberService.memberExistsWithSalesforceIdAndSuperadminEnabled(Mockito.anyString())).thenReturn(Boolean.FALSE);
        Mockito.when(mockedMemberService.getMemberNameBySalesforce(Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(DEFAULT_MEMBER_NAME);
        Mockito.when(mockedMemberService.getMemberNameBySalesforce(Mockito.eq(UPDATED_SALESFORCE_ID))).thenReturn(UPDATED_MEMBER_NAME);
        ReflectionTestUtils.setField(userService, "memberService", mockedMemberService);
        ReflectionTestUtils.setField(userMapper, "memberService", mockedMemberService);
        ReflectionTestUtils.setField(userService, "mailService", Mockito.mock(MailService.class));
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
        user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
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
        user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
        return user;
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
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

        restUserMockMvc.perform(post("/api/users").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
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
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
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
        restUserMockMvc.perform(post("/api/users").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
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
        restUserMockMvc.perform(post("/api/users").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
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
        restUserMockMvc.perform(post("/api/users").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
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
        managedUserVM.setSalesforceId("salesforceId");
        managedUserVM.setMainContact(true);
        managedUserVM.setIsAdmin(true);
        managedUserVM.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Mocked member does not have superadmin enabled so this request
        // must fail
        restUserMockMvc.perform(post("/api/users").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getAllUsers() throws Exception {
        // Initialize the database
        userRepository.save(user);

        restUserMockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
                .andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LASTNAME))).andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
                .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGEURL))).andExpect(jsonPath("$.[*].langKey").value(hasItem(DEFAULT_LANGKEY)));
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getUser() throws Exception {
        // Initialize the database
        userRepository.save(user);

        assertThat(cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get(user.getEmail())).isNull();

        restUserMockMvc.perform(get("/api/users/{login}/", user.getEmail())).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME)).andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
                .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGEURL)).andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY));

        assertThat(cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get(user.getEmail())).isNotNull();
    }

    @Test
    public void getNonExistingUser() throws Exception {
        restUserMockMvc.perform(get("/api/users/unknown")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
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

        restUserMockMvc.perform(put("/api/users").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
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
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getUsersBySalesforceId() throws Exception {
        saveTenUsersWithSalesforceId("salesforce-id-1");
        userRepository.save(user);

        MvcResult result = restUserMockMvc
                .perform(get("/api/users/salesforce/salesforce-id-1/p").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andReturn();

        List<UserDTO> users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(10);

        result = restUserMockMvc
                .perform(get("/api/users/salesforce/incorrect-salesforce-id/p").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users).isEmpty();

        result = restUserMockMvc.perform(get("/api/users/salesforce/salesforce-id-1/p").param("filter", "firstname").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(10);
        
        result = restUserMockMvc.perform(get("/api/users/salesforce/salesforce-id-1/p").param("filter", "firstname 1").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(1);
        
        result = restUserMockMvc.perform(get("/api/users/salesforce/salesforce-id-1/p").param("filter", "lastname 2").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(1);
        
        result = restUserMockMvc.perform(get("/api/users/salesforce/salesforce-id-1/p").param("filter", "membername 3").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(1);
        
        result = restUserMockMvc.perform(get("/api/users/salesforce/salesforce-id-1/p").param("filter", "4@salesforce-id-1.org").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(1);
        
        // bad salesforce id
        result = restUserMockMvc.perform(get("/api/users/salesforce/salesforce-id-5/p").param("filter", "4@orcid.org").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(0);
    }
    
    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_USER", "ROLE_ADMIN" }, password = LOGGED_IN_PASSWORD)
    public void getAllUsersWithFilter() throws Exception {
        saveTenUsersWithSalesforceId("salesforce-id-1");
        saveTenUsersWithSalesforceId("salesforce-id-2");
        saveTenUsersWithSalesforceId("salesforce-id-3");
        userRepository.save(user);

        MvcResult result = restUserMockMvc.perform(get("/api/users").param("filter", "firstname").param("size", "50").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        List<UserDTO> users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(30);
        
        result = restUserMockMvc.perform(get("/api/users").param("filter", "firstname 1").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(3);
        
        result = restUserMockMvc.perform(get("/api/users").param("filter", "lastname 2").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(3);
        
        result = restUserMockMvc.perform(get("/api/users").param("filter", "membername 3").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(3);
        
        result = restUserMockMvc.perform(get("/api/users").param("filter", "4@salesforce-id-1.org").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        users = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<UserDTO>>() {
        });
        assertThat(users.size()).isEqualTo(1);
    }

    private void saveTenUsersWithSalesforceId(String salesforceId) {
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setPassword(RandomStringUtils.random(60));
            user.setActivated(true);
            user.setEmail(i + "@" + salesforceId + ".org");
            user.setFirstName("firstname " + i);
            user.setLastName("lastname " + i);
            user.setMemberName("membername " + i);
            user.setImageUrl(DEFAULT_IMAGEURL);
            user.setLangKey(DEFAULT_LANGKEY);
            user.setSalesforceId(salesforceId);
            user.setMainContact(false);
            user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
            userRepository.save(user);
        }
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USR" }, password = LOGGED_IN_PASSWORD)
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
        managedUserVM.setSalesforceId("salesforceId");
        managedUserVM.setIsAdmin(true);

        restUserMockMvc.perform(put("/api/users").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getAllAuthorities() throws Exception {
        restUserMockMvc.perform(get("/api/users/authorities").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasItems(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN)));
    }

    @Test
    public void testUserEquals() throws Exception {
        TestUtil.equalsVerifier(User.class);
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

    @Test
    public void testAuthorityEquals() {
        Authority authorityA = new Authority();
        assertThat(authorityA).isEqualTo(authorityA);
        assertThat(authorityA).isNotEqualTo(null);
        assertThat(authorityA).isNotEqualTo(new Object());
        assertThat(authorityA.hashCode()).isEqualTo(0);
        assertThat(authorityA.toString()).isNotNull();

        Authority authorityB = new Authority();
        assertThat(authorityA).isEqualTo(authorityB);

        authorityB.setName(AuthoritiesConstants.ADMIN);
        assertThat(authorityA).isNotEqualTo(authorityB);

        authorityA.setName(AuthoritiesConstants.USER);
        assertThat(authorityA).isNotEqualTo(authorityB);

        authorityB.setName(AuthoritiesConstants.USER);
        assertThat(authorityA).isEqualTo(authorityB);
        assertThat(authorityA.hashCode()).isEqualTo(authorityB.hashCode());
    }
}
