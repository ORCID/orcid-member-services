package org.orcid.user.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.orcid.user.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.UserSettingsServiceApp;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.config.SecurityBeanOverrideConfiguration;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.MemberSettingsRepository;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.security.UaaUserUtils;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.web.rest.errors.ExceptionTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.web.server.ResponseStatusException;

import com.netflix.hystrix.exception.HystrixRuntimeException;

/**
 * Integration tests for the {@link UserSettingsResource} REST controller.
 */
@SpringBootTest(classes = { SecurityBeanOverrideConfiguration.class, UserSettingsServiceApp.class })
public class UserSettingsResourceIT {

    private static final String DEFAULT_LOGIN = "AAAAAAAAAA";
    private static final String UPDATED_LOGIN = "BBBBBBBBBB";

    private static final String DEFAULT_JHI_USER_ID = "AAAAAAAAAA";
    private static final String UPDATED_JHI_USER_ID = "BBBBBBBBBB";

    private static final String DEFAULT_SALESFORCE_ID = "AAAAAAAAAA";
    private static final String UPDATED_SALESFORCE_ID = "BBBBBBBBBB";

    private static final Boolean DEFAULT_MAIN_CONTACT = false;
    private static final Boolean UPDATED_MAIN_CONTACT = true;

    private static final String DEFAULT_CREATED_BY = "AAAAAAAAAA";
    private static final String UPDATED_CREATED_BY = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_LAST_MODIFIED_BY = "AAAAAAAAAA";
    private static final String UPDATED_LAST_MODIFIED_BY = "BBBBBBBBBB";

    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_MODIFIED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private MemberSettingsRepository memberSettingsRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private Validator validator;

    @Mock
    private Oauth2ServiceClient oauth2ServiceClient;

    @Mock
    private UaaUserUtils mockUaaUserUtils;

    private MockMvc restUserSettingsMockMvc;

    private UserDTO userSettings;

    @BeforeEach
    public void setup() throws JSONException {
        MockitoAnnotations.initMocks(this);
        ResponseEntity<Void> createdResponse = new ResponseEntity<Void>(HttpStatus.CREATED);

        JSONObject obj = getJSONUser();

        ResponseEntity<String> getUserResponse = new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
        when(oauth2ServiceClient.registerUser(Mockito.anyMap())).thenReturn(createdResponse);
        when(oauth2ServiceClient.getUser(DEFAULT_LOGIN)).thenReturn(getUserResponse);

        when(mockUaaUserUtils.getAuthenticatedUaaUserId()).thenReturn(DEFAULT_JHI_USER_ID);
        when(mockUaaUserUtils.getUAAUserById(DEFAULT_JHI_USER_ID)).thenReturn(obj);

        final UserSettingsResource userSettingsResource = new UserSettingsResource(userSettingsRepository, memberSettingsRepository);
        userSettingsResource.setOauth2ServiceClient(oauth2ServiceClient);
        userSettingsResource.setUaaUserUtils(mockUaaUserUtils);
        this.restUserSettingsMockMvc = MockMvcBuilders.standaloneSetup(userSettingsResource).setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator).setConversionService(createFormattingConversionService()).setMessageConverters(jacksonMessageConverter)
                .setValidator(validator).build();
    }

    private JSONObject getJSONUser() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("login", DEFAULT_LOGIN);
        obj.put("createdDate", Instant.now().toString());
        obj.put("lastModifiedBy", DEFAULT_LOGIN);
        obj.put("lastModifiedDate", Instant.now().toString());
        obj.put("firstName", "firstName");
        obj.put("lastName", "lastName");
        obj.put("authorities", Lists.emptyList());
        return obj;
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserDTO createEntity() {
        UserDTO userSettings = new UserDTO();
        userSettings.setSalesforceId(DEFAULT_SALESFORCE_ID);
        userSettings.setCreatedBy(DEFAULT_CREATED_BY);
        userSettings.setCreatedDate(DEFAULT_CREATED_DATE);
        userSettings.setLastModifiedBy(DEFAULT_LAST_MODIFIED_BY);
        userSettings.setLastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
        userSettings.setJhiUserId(DEFAULT_JHI_USER_ID);
        userSettings.setLogin(DEFAULT_LOGIN);
        userSettings.setMainContact(DEFAULT_MAIN_CONTACT);
        return userSettings;
    }

    @BeforeEach
    public void initTest() {
        userSettingsRepository.deleteAll();
        userSettings = createEntity();
    }

    @Test
    @WithMockUser(username = UPDATED_LAST_MODIFIED_BY, authorities = { "ROLE_ADMIN", "ROLE_USR" }, password = "user")
    public void createUserSettings() throws Exception {
        int databaseSizeBeforeCreate = userSettingsRepository.findAll().size();

        JSONObject obj = getJSONUser();
        obj.put("id", DEFAULT_JHI_USER_ID);
        ResponseEntity<String> getUserResponse = new ResponseEntity<String>(obj.toString(), HttpStatus.OK);

        // Throw not found on getUser the first time
        ResponseStatusException rse = new ResponseStatusException(HttpStatus.NOT_FOUND);
        HystrixRuntimeException hre = new HystrixRuntimeException(null, null, null, rse, null);
        when(oauth2ServiceClient.getUser(DEFAULT_LOGIN)).thenThrow(hre).thenReturn(getUserResponse);

        // Create the UserSettings
        restUserSettingsMockMvc.perform(post("/settings/api/user").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(userSettings)))
                .andExpect(status().isCreated());

        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeCreate + 1);
        UserSettings testUserSettings = userSettingsList.get(userSettingsList.size() - 1);
        assertThat(testUserSettings.getJhiUserId()).isEqualTo(DEFAULT_JHI_USER_ID);
        assertThat(testUserSettings.getMainContact()).isEqualTo(DEFAULT_MAIN_CONTACT);
        assertThat(testUserSettings.getSalesforceId()).isEqualTo(DEFAULT_SALESFORCE_ID);
        assertThat(testUserSettings.getCreatedBy()).isEqualTo(DEFAULT_CREATED_BY);
        assertThat(testUserSettings.getLastModifiedBy()).isEqualTo(DEFAULT_LAST_MODIFIED_BY);
        assertNotNull(testUserSettings.getCreatedDate());
        assertNotNull(testUserSettings.getLastModifiedDate());
    }

    @Test
    @WithMockUser(username = UPDATED_LAST_MODIFIED_BY, authorities = { "ROLE_ADMIN", "ROLE_USR" }, password = "user")
    public void clearUserSettings() throws Exception {
        ResponseEntity<String> clearUserResponse = new ResponseEntity<String>(StringUtils.EMPTY, HttpStatus.OK);
        when(oauth2ServiceClient.clearUser(DEFAULT_JHI_USER_ID)).thenReturn(clearUserResponse);

        // Initialize the database
        userSettingsRepository.save(UserSettings.valueOf(userSettings));

        // Delete the userSettings
        restUserSettingsMockMvc.perform(delete("/settings/api/user/{id}", userSettings.getJhiUserId()).accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isAccepted());

        // Validate the database contains one less item
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(1);
        UserSettings us = userSettingsList.get(0);
        assertNotNull(us);
        assertNotNull(us.getId());
        assertNotNull(us.getJhiUserId());
        assertNotNull(us.getLastModifiedBy());
        assertNotNull(us.getLastModifiedDate());
        assertNotNull(us.getCreatedBy());
        assertNotNull(us.getCreatedDate());

        assertTrue(us.getDeleted());
        assertNull(us.getSalesforceId());
    }

    @Test
    @WithMockUser(username = UPDATED_LAST_MODIFIED_BY, authorities = { "ROLE_ADMIN", "ROLE_USR" }, password = "user")
    public void createUserSettingsWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userSettingsRepository.findAll().size();

        // Create the UserSettings with an existing ID
        userSettings.setId("existing_id");

        // An entity with an existing ID cannot be created, so this API call
        // must fail
        restUserSettingsMockMvc.perform(post("/settings/api/user").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(userSettings)))
                .andExpect(status().isBadRequest());

        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void getAllUserSettings() throws Exception {
        // Initialize the database
        UserSettings us = userSettingsRepository.save(UserSettings.valueOf(userSettings));

        String id = us.getId();

        // Get all the userSettingsList
        restUserSettingsMockMvc.perform(get("/settings/api/users?sort=id,desc")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(id))
                .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN))).andExpect(jsonPath("$.[*].salesforceId").value(hasItem(DEFAULT_SALESFORCE_ID)))
                .andExpect(jsonPath("$.[*].mainContact").value(hasItem(DEFAULT_MAIN_CONTACT.booleanValue())))
                .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY)))
                .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
                .andExpect(jsonPath("$.[*].lastModifiedBy").value(hasItem(DEFAULT_LAST_MODIFIED_BY)))
                .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }

    @Test
    public void getUserSettings() throws Exception {
        // Initialize the database
        UserSettings us = userSettingsRepository.save(UserSettings.valueOf(userSettings));
        String id = us.getId();
        
        // Get the userSettings
        restUserSettingsMockMvc.perform(get("/settings/api/user/{jhiUserId}", userSettings.getJhiUserId())).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.login").value(DEFAULT_JHI_USER_ID))
                .andExpect(jsonPath("$.salesforceId").value(DEFAULT_SALESFORCE_ID))
                .andExpect(jsonPath("$.mainContact").value(DEFAULT_MAIN_CONTACT.booleanValue()))
                .andExpect(jsonPath("$.createdBy").value(DEFAULT_CREATED_BY))
                .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
                .andExpect(jsonPath("$.lastModifiedBy").value(DEFAULT_LAST_MODIFIED_BY))
                .andExpect(jsonPath("$.lastModifiedDate").value(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    public void getNonExistingUserSettings() throws Exception {
        // Get the userSettings
        restUserSettingsMockMvc.perform(get("/settings/api/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = UPDATED_LAST_MODIFIED_BY, authorities = { "ROLE_ADMIN", "ROLE_USR" }, password = "user")
    public void updateUserSettings() throws Exception {                
        when(oauth2ServiceClient.updateUser(Mockito.anyMap())).thenReturn(new ResponseEntity<String>(getJSONUser().toString(), HttpStatus.OK));
        // Initialize the database
        UserSettings us = userSettingsRepository.save(UserSettings.valueOf(userSettings));

        int databaseSizeBeforeUpdate = userSettingsRepository.findAll().size();

        // Update the userSettings
        UserSettings updatedUserSettings = userSettingsRepository.findById(us.getId()).get();
        updatedUserSettings.setSalesforceId(UPDATED_SALESFORCE_ID);
        updatedUserSettings.setCreatedBy(UPDATED_CREATED_BY);
        updatedUserSettings.setCreatedDate(UPDATED_CREATED_DATE);
        updatedUserSettings.setLastModifiedBy(UPDATED_LAST_MODIFIED_BY);
        updatedUserSettings.setLastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        updatedUserSettings.setJhiUserId(UPDATED_JHI_USER_ID);
        updatedUserSettings.setMainContact(UPDATED_MAIN_CONTACT);

        restUserSettingsMockMvc.perform(put("/settings/api/user")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedUserSettings)))
            .andExpect(status().isOk());

        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeUpdate);
        UserSettings testUserSettings = userSettingsList.get(userSettingsList.size() - 1);
        assertThat(testUserSettings.getJhiUserId()).isEqualTo(DEFAULT_JHI_USER_ID);
        assertThat(testUserSettings.getMainContact()).isEqualTo(DEFAULT_MAIN_CONTACT);
        assertThat(testUserSettings.getSalesforceId()).isEqualTo(DEFAULT_SALESFORCE_ID);
        assertThat(testUserSettings.getCreatedBy()).isEqualTo(UPDATED_CREATED_BY);
        assertThat(testUserSettings.getCreatedDate()).isEqualTo(UPDATED_CREATED_DATE);
        assertThat(testUserSettings.getLastModifiedBy()).isEqualTo(UPDATED_LAST_MODIFIED_BY);
        assertThat(testUserSettings.getLastModifiedDate()).isEqualTo(UPDATED_LAST_MODIFIED_DATE);
    }

    @Test
    public void updateNonExistingUserSettings() throws Exception {
        int databaseSizeBeforeUpdate = userSettingsRepository.findAll().size();

        // Create the UserSettings

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserSettingsMockMvc.perform(put("/settings/api/user")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userSettings)))
            .andExpect(status().isBadRequest());

        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeUpdate);
    }
    
    @Test
    public void equalsVerifier() throws Exception {
        UserSettings userSettings1 = new UserSettings();
        userSettings1.setId("id1");
        UserSettings userSettings2 = new UserSettings();
        userSettings2.setId(userSettings1.getId());
        assertThat(userSettings1).isEqualTo(userSettings2);
        userSettings2.setId("id2");
        assertThat(userSettings1).isNotEqualTo(userSettings2);
        userSettings1.setId(null);
        assertThat(userSettings1).isNotEqualTo(userSettings2);
    }
}
