package org.orcid.user.web.rest;

import org.orcid.user.UserSettingsServiceApp;
import org.orcid.user.config.SecurityBeanOverrideConfiguration;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.orcid.user.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link UserSettingsResource} REST controller.
 */
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, UserSettingsServiceApp.class})
public class UserSettingsResourceIT {

    private static final String DEFAULT_LOGIN = "AAAAAAAAAA";
    private static final String UPDATED_LOGIN = "BBBBBBBBBB";

    private static final String DEFAULT_SALESFORCE_ID = "AAAAAAAAAA";
    private static final String UPDATED_SALESFORCE_ID = "BBBBBBBBBB";

    private static final Boolean DEFAULT_DISABLED = false;
    private static final Boolean UPDATED_DISABLED = true;

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
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private Validator validator;

    private MockMvc restUserSettingsMockMvc;

    private UserSettings userSettings;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final UserSettingsResource userSettingsResource = new UserSettingsResource(userSettingsRepository);
        this.restUserSettingsMockMvc = MockMvcBuilders.standaloneSetup(userSettingsResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserSettings createEntity() {
        UserSettings userSettings = new UserSettings();
        userSettings.setSalesforceId(DEFAULT_SALESFORCE_ID);
        userSettings.setCreatedBy(DEFAULT_CREATED_BY);
        userSettings.setCreatedDate(DEFAULT_CREATED_DATE);
        userSettings.setLastModifiedBy(DEFAULT_LAST_MODIFIED_BY);
        userSettings.setLastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
        userSettings.setDisabled(DEFAULT_DISABLED);
        userSettings.setLogin(DEFAULT_LOGIN);
        userSettings.setMainContact(DEFAULT_MAIN_CONTACT);
        return userSettings;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserSettings createUpdatedEntity() {
        UserSettings userSettings = new UserSettings();
        userSettings.setSalesforceId(UPDATED_SALESFORCE_ID);
        userSettings.setCreatedBy(UPDATED_CREATED_BY);
        userSettings.setCreatedDate(UPDATED_CREATED_DATE);
        userSettings.setLastModifiedBy(UPDATED_LAST_MODIFIED_BY);
        userSettings.setLastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        userSettings.setDisabled(UPDATED_DISABLED);
        userSettings.setLogin(UPDATED_LOGIN);
        userSettings.setMainContact(UPDATED_MAIN_CONTACT);
        return userSettings;
    }

    @BeforeEach
    public void initTest() {
        userSettingsRepository.deleteAll();
        userSettings = createEntity();
    }

    @Test
    public void createUserSettings() throws Exception {
        int databaseSizeBeforeCreate = userSettingsRepository.findAll().size();

        // Create the UserSettings
        restUserSettingsMockMvc.perform(post("/settings/api/user")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userSettings)))
            .andExpect(status().isCreated());

        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeCreate + 1);
        UserSettings testUserSettings = userSettingsList.get(userSettingsList.size() - 1);
        assertThat(testUserSettings.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(testUserSettings.getDisabled()).isEqualTo(DEFAULT_DISABLED);
        assertThat(testUserSettings.getMainContact()).isEqualTo(DEFAULT_MAIN_CONTACT);
        assertThat(testUserSettings.getSalesforceId()).isEqualTo(DEFAULT_SALESFORCE_ID);
        assertThat(testUserSettings.getCreatedBy()).isEqualTo(DEFAULT_CREATED_BY);
        assertThat(testUserSettings.getCreatedDate()).isEqualTo(DEFAULT_CREATED_DATE);
        assertThat(testUserSettings.getLastModifiedBy()).isEqualTo(DEFAULT_LAST_MODIFIED_BY);
        assertThat(testUserSettings.getLastModifiedDate()).isEqualTo(DEFAULT_LAST_MODIFIED_DATE);
    }

    @Test
    public void createUserSettingsWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userSettingsRepository.findAll().size();

        // Create the UserSettings with an existing ID
        userSettings.setId("existing_id");

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserSettingsMockMvc.perform(post("/settings/api/user")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userSettings)))
            .andExpect(status().isBadRequest());

        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void getAllUserSettings() throws Exception {
        // Initialize the database
        userSettingsRepository.save(userSettings);

        // Get all the userSettingsList
        restUserSettingsMockMvc.perform(get("/settings/api/users?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userSettings.getId())))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))            
            .andExpect(jsonPath("$.[*].salesforceId").value(hasItem(DEFAULT_SALESFORCE_ID)))
            .andExpect(jsonPath("$.[*].disabled").value(hasItem(DEFAULT_DISABLED.booleanValue())))
            .andExpect(jsonPath("$.[*].mainContact").value(hasItem(DEFAULT_MAIN_CONTACT.booleanValue())))
            .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedBy").value(hasItem(DEFAULT_LAST_MODIFIED_BY)))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }
    
    @Test
    public void getUserSettings() throws Exception {
        // Initialize the database
        userSettingsRepository.save(userSettings);

        // Get the userSettings
        restUserSettingsMockMvc.perform(get("/settings/api/{id}", userSettings.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userSettings.getId())))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))            
            .andExpect(jsonPath("$.[*].salesforceId").value(hasItem(DEFAULT_SALESFORCE_ID)))
            .andExpect(jsonPath("$.[*].disabled").value(hasItem(DEFAULT_DISABLED.booleanValue())))
            .andExpect(jsonPath("$.[*].mainContact").value(hasItem(DEFAULT_MAIN_CONTACT.booleanValue())))
            .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedBy").value(hasItem(DEFAULT_LAST_MODIFIED_BY)))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }

    @Test
    public void getNonExistingUserSettings() throws Exception {
        // Get the userSettings
        restUserSettingsMockMvc.perform(get("/settings/api/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateUserSettings() throws Exception {
        // Initialize the database
        userSettingsRepository.save(userSettings);

        int databaseSizeBeforeUpdate = userSettingsRepository.findAll().size();

        // Update the userSettings
        UserSettings updatedUserSettings = userSettingsRepository.findById(userSettings.getId()).get();
        updatedUserSettings.setSalesforceId(UPDATED_SALESFORCE_ID);
        updatedUserSettings.setCreatedBy(UPDATED_CREATED_BY);
        updatedUserSettings.setCreatedDate(UPDATED_CREATED_DATE);
        updatedUserSettings.setLastModifiedBy(UPDATED_LAST_MODIFIED_BY);
        updatedUserSettings.setLastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        updatedUserSettings.setDisabled(UPDATED_DISABLED);
        updatedUserSettings.setLogin(UPDATED_LOGIN);
        updatedUserSettings.setMainContact(UPDATED_MAIN_CONTACT);

        restUserSettingsMockMvc.perform(put("/settings/api/user")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedUserSettings)))
            .andExpect(status().isOk());

        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeUpdate);
        UserSettings testUserSettings = userSettingsList.get(userSettingsList.size() - 1);
        assertThat(testUserSettings.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(testUserSettings.getDisabled()).isEqualTo(DEFAULT_DISABLED);
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
    public void deleteUserSettings() throws Exception {
        // Initialize the database
        userSettingsRepository.save(userSettings);

        int databaseSizeBeforeDelete = userSettingsRepository.findAll().size();

        // Delete the userSettings
        restUserSettingsMockMvc.perform(delete("/settings/api/user/{id}", userSettings.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserSettings.class);
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
