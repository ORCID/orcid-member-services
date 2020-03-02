package org.orcid.user.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.orcid.user.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
import org.orcid.user.security.SecurityUtils;
import org.orcid.user.web.rest.errors.ExceptionTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

/**
 * Integration tests for the {@link UserSettingsResource} REST controller.
 */
@SpringBootTest(properties = { "feign.hystrix.enabled=true"}, classes = {SecurityBeanOverrideConfiguration.class, UserSettingsServiceApp.class})
public class UserSettingsResourceIT {

    private static final String DEFAULT_LOGIN = "AAAAAAAAAA";
    private static final String UPDATED_LOGIN = "BBBBBBBBBB";

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

    private MockMvc restUserSettingsMockMvc;

    private UserSettings userSettings;

    @BeforeEach
    public void setup() throws JSONException {
        MockitoAnnotations.initMocks(this);
        ResponseEntity<Void> createdResponse = new ResponseEntity<Void>(HttpStatus.CREATED);
        
        JSONObject obj = new JSONObject();
        obj.put("login", DEFAULT_LOGIN);
        obj.put("createdDate", Instant.now().toString());
        obj.put("lastModifiedBy", DEFAULT_LOGIN);
        obj.put("lastModifiedDate", Instant.now().toString());
                
        ResponseEntity<String> getUserResponse = new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
        when(oauth2ServiceClient.registerUser(Mockito.anyMap())).thenReturn(createdResponse);
        when(oauth2ServiceClient.getUser(DEFAULT_LOGIN)).thenReturn(getUserResponse);
        final UserSettingsResource userSettingsResource = new UserSettingsResource(userSettingsRepository, memberSettingsRepository);
        userSettingsResource.setOauth2ServiceClient(oauth2ServiceClient);
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
    @WithMockUser(username=DEFAULT_LOGIN,authorities={"ROLE_ADMIN", "ROLE_USR"}, password = "user")
    public void createUserSettings() throws Exception {
        int databaseSizeBeforeCreate = userSettingsRepository.findAll().size();

        // Create the UserSettings
        restUserSettingsMockMvc.perform(post("/settings/api/user")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userSettings)))
            .andExpect(status().isCreated());

        int databaseSizeAfterCreate = databaseSizeBeforeCreate + 1;
        
        // Validate the UserSettings in the database
        List<UserSettings> userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeAfterCreate);
        UserSettings testUserSettings = userSettingsList.get(userSettingsList.size() - 1);
        assertThat(testUserSettings.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(testUserSettings.getMainContact()).isEqualTo(DEFAULT_MAIN_CONTACT);
        assertThat(testUserSettings.getSalesforceId()).isEqualTo(DEFAULT_SALESFORCE_ID);
        assertThat(testUserSettings.getCreatedBy()).isEqualTo(DEFAULT_CREATED_BY);
        assertThat(testUserSettings.getLastModifiedBy()).isEqualTo(DEFAULT_LAST_MODIFIED_BY);
        assertNotNull(testUserSettings.getCreatedDate());
        assertNotNull(testUserSettings.getLastModifiedDate());
        
        
        // An entity with an existing ID cannot be created, so this API call must fail
        restUserSettingsMockMvc.perform(post("/settings/api/user")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userSettings)))
            .andExpect(status().isBadRequest());

        // Validate the UserSettings in the database
        userSettingsList = userSettingsRepository.findAll();
        assertThat(userSettingsList).hasSize(databaseSizeAfterCreate);
    }        
    
}
