package org.orcid.user.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.orcid.user.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.user.UserSettingsServiceApp;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.config.SecurityBeanOverrideConfiguration;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.repository.MemberSettingsRepository;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.security.UaaUserUtils;
import org.orcid.user.web.rest.errors.ExceptionTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

/**
 * Integration tests for the {@link MemberSettingsResource} REST controller.
 */
@SpringBootTest(classes = { SecurityBeanOverrideConfiguration.class, UserSettingsServiceApp.class })
public class MemberSettingsResourceIT {

    private static final String DEFAULT_CLIENT_ID = "AAAAAAAAAA";
    private static final String UPDATED_CLIENT_ID = "BBBBBBBBBB";

    private static final String DEFAULT_SALESFORCE_ID = "AAAAAAAAAA";
    private static final String UPDATED_SALESFORCE_ID = "BBBBBBBBBB";

    private static final String DEFAULT_PARENT_SALESFORCE_ID = "AAAAAAAAAA";
    private static final String UPDATED_PARENT_SALESFORCE_ID = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ASSERTION_SERVICE_ENABLED = false;
    private static final Boolean UPDATED_ASSERTION_SERVICE_ENABLED = true;

    private static final String DEFAULT_CREATED_BY = "AAAAAAAAAA";
    private static final String UPDATED_CREATED_BY = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_LAST_MODIFIED_BY = "AAAAAAAAAA";
    private static final String UPDATED_LAST_MODIFIED_BY = "BBBBBBBBBB";

    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.ofEpochMilli(0L);
    

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
    private UaaUserUtils mockUaaUserUtils;
    
    private MockMvc restMemberSettingsMockMvc;

    private MemberSettings memberSettings;

    @BeforeEach
    public void setup() throws JSONException {
        MockitoAnnotations.initMocks(this);
        
        when(mockUaaUserUtils.getAuthenticatedUaaUserId()).thenReturn(DEFAULT_CREATED_BY);
        
        final MemberSettingsResource memberSettingsResource = new MemberSettingsResource(memberSettingsRepository, userSettingsRepository);
        memberSettingsResource.setUaaUserUtils(mockUaaUserUtils);
        this.restMemberSettingsMockMvc = MockMvcBuilders.standaloneSetup(memberSettingsResource).setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator).setConversionService(createFormattingConversionService()).setMessageConverters(jacksonMessageConverter)
                .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MemberSettings createEntity() {
        MemberSettings memberSettings = new MemberSettings().clientId(DEFAULT_CLIENT_ID).salesforceId(DEFAULT_SALESFORCE_ID)
                .parentSalesforceId(DEFAULT_PARENT_SALESFORCE_ID).assertionServiceEnabled(DEFAULT_ASSERTION_SERVICE_ENABLED).createdBy(DEFAULT_CREATED_BY)
                .createdDate(DEFAULT_CREATED_DATE).lastModifiedBy(DEFAULT_LAST_MODIFIED_BY).lastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);

        memberSettings.setIsConsortiumLead(false);        
        return memberSettings;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MemberSettings createUpdatedEntity() {
        MemberSettings memberSettings = new MemberSettings().clientId(UPDATED_CLIENT_ID).salesforceId(UPDATED_SALESFORCE_ID)
                .parentSalesforceId(UPDATED_PARENT_SALESFORCE_ID).assertionServiceEnabled(UPDATED_ASSERTION_SERVICE_ENABLED).createdBy(UPDATED_CREATED_BY)
                .createdDate(UPDATED_CREATED_DATE).lastModifiedBy(UPDATED_LAST_MODIFIED_BY);
        return memberSettings;
    }

    @BeforeEach
    public void initTest() {
        memberSettingsRepository.deleteAll();
        memberSettings = createEntity();
    }

    @Test
    public void createMemberSettings() throws Exception {
        int databaseSizeBeforeCreate = memberSettingsRepository.findAll().size();

        // Create the MemberSettings
        restMemberSettingsMockMvc
                .perform(post("/settings/api/member-settings").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(memberSettings)))
                .andExpect(status().isCreated());

        // Validate the MemberSettings in the database
        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeCreate + 1);
        MemberSettings testMemberSettings = memberSettingsList.get(memberSettingsList.size() - 1);
        assertThat(testMemberSettings.getClientId()).isEqualTo(DEFAULT_CLIENT_ID);
        assertThat(testMemberSettings.getSalesforceId()).isEqualTo(DEFAULT_SALESFORCE_ID);
        assertThat(testMemberSettings.getParentSalesforceId()).isEqualTo(DEFAULT_PARENT_SALESFORCE_ID);
        assertThat(testMemberSettings.isAssertionServiceEnabled()).isEqualTo(DEFAULT_ASSERTION_SERVICE_ENABLED);
        assertThat(testMemberSettings.getCreatedBy()).isEqualTo(DEFAULT_CREATED_BY);
        assertNotNull(testMemberSettings.getCreatedDate());
        assertThat(testMemberSettings.getLastModifiedBy()).isEqualTo(DEFAULT_LAST_MODIFIED_BY);
        assertNotNull(testMemberSettings.getLastModifiedDate());
    }

    @Test
    public void createMemberSettingsWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = memberSettingsRepository.findAll().size();

        // Create the MemberSettings with an existing ID
        memberSettings.setId("existing_id");

        // An entity with an existing ID cannot be created, so this API call
        // must fail
        restMemberSettingsMockMvc
                .perform(post("/settings/api/member-settings").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(memberSettings)))
                .andExpect(status().isBadRequest());

        // Validate the MemberSettings in the database
        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkClientIdIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = memberSettingsRepository.findAll().size();
        // set the field null
        memberSettings.setClientId(null);

        // Create the MemberSettings, which fails.

        restMemberSettingsMockMvc
                .perform(post("/settings/api/member-settings").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(memberSettings)))
                .andExpect(status().isCreated());

        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeTest + 1);
    }

    @Test
    public void checkCreatedByIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = memberSettingsRepository.findAll().size();
        // set the field null
        memberSettings.setCreatedBy(null);

        // Create the MemberSettings, which fails.

        restMemberSettingsMockMvc
                .perform(post("/settings/api/member-settings").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(memberSettings)))
                .andExpect(status().isCreated());

        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeTest + 1);
    }

    @Test
    public void checkCreatedDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = memberSettingsRepository.findAll().size();
        // set the field null
        memberSettings.setCreatedDate(null);

        // Create the MemberSettings, which fails.

        restMemberSettingsMockMvc
                .perform(post("/settings/api/member-settings").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(memberSettings)))
                .andExpect(status().isCreated());

        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeTest + 1);
    }

    @Test
    public void checkLastModifiedByIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = memberSettingsRepository.findAll().size();
        // set the field null
        memberSettings.setLastModifiedBy(null);

        // Create the MemberSettings, which fails.

        restMemberSettingsMockMvc
                .perform(post("/settings/api/member-settings").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(memberSettings)))
                .andExpect(status().isCreated());

        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeTest + 1);
    }

    @Test
    public void checkLastModifiedDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = memberSettingsRepository.findAll().size();
        // set the field null
        memberSettings.setLastModifiedDate(null);

        // Create the MemberSettings, which fails.

        restMemberSettingsMockMvc
                .perform(post("/settings/api/member-settings").contentType(TestUtil.APPLICATION_JSON_UTF8).content(TestUtil.convertObjectToJsonBytes(memberSettings)))
                .andExpect(status().isCreated());

        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeTest + 1);
    }
    
    @Test
    public void getAllMemberSettings() throws Exception {
        // Initialize the database
        memberSettingsRepository.save(memberSettings);

        // Get all the memberSettingsList
        restMemberSettingsMockMvc.perform(get("/settings/api/member-settings?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(memberSettings.getId())))
            .andExpect(jsonPath("$.[*].clientId").value(hasItem(DEFAULT_CLIENT_ID)))
            .andExpect(jsonPath("$.[*].salesforceId").value(hasItem(DEFAULT_SALESFORCE_ID)))
            .andExpect(jsonPath("$.[*].parentSalesforceId").value(hasItem(DEFAULT_PARENT_SALESFORCE_ID)))
            .andExpect(jsonPath("$.[*].assertionServiceEnabled").value(hasItem(DEFAULT_ASSERTION_SERVICE_ENABLED.booleanValue())))
            .andExpect(jsonPath("$.[*].createdBy").value(hasItem(DEFAULT_CREATED_BY)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedBy").value(hasItem(DEFAULT_LAST_MODIFIED_BY)))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }
    
    @Test
    public void getMemberSettings() throws Exception {
        // Initialize the database
        memberSettingsRepository.save(memberSettings);

        // Get the memberSettings
        restMemberSettingsMockMvc.perform(get("/settings/api/member-settings/{id}", memberSettings.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(memberSettings.getId()))
            .andExpect(jsonPath("$.clientId").value(DEFAULT_CLIENT_ID))
            .andExpect(jsonPath("$.salesforceId").value(DEFAULT_SALESFORCE_ID))
            .andExpect(jsonPath("$.parentSalesforceId").value(DEFAULT_PARENT_SALESFORCE_ID))
            .andExpect(jsonPath("$.assertionServiceEnabled").value(DEFAULT_ASSERTION_SERVICE_ENABLED.booleanValue()))
            .andExpect(jsonPath("$.createdBy").value(DEFAULT_CREATED_BY))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.lastModifiedBy").value(DEFAULT_LAST_MODIFIED_BY))
            .andExpect(jsonPath("$.lastModifiedDate").value(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    public void getNonExistingMemberSettings() throws Exception {
        // Get the memberSettings
        restMemberSettingsMockMvc.perform(get("/settings/api/member-settings/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username=UPDATED_LAST_MODIFIED_BY,authorities={"ROLE_ADMIN", "ROLE_USR"}, password = "user")
    public void updateMemberSettings() throws Exception {
        when(mockUaaUserUtils.getAuthenticatedUaaUserId()).thenReturn(UPDATED_LAST_MODIFIED_BY);
        
        // Initialize the database
        memberSettingsRepository.save(memberSettings);
        
        int databaseSizeBeforeUpdate = memberSettingsRepository.findAll().size();

        // Update the memberSettings
        MemberSettings updatedMemberSettings = memberSettingsRepository.findById(memberSettings.getId()).get();
        Instant initialLastModified = updatedMemberSettings.getLastModifiedDate();
        updatedMemberSettings
            .clientId(UPDATED_CLIENT_ID)
            .salesforceId(UPDATED_SALESFORCE_ID)
            .parentSalesforceId(UPDATED_PARENT_SALESFORCE_ID)
            .assertionServiceEnabled(UPDATED_ASSERTION_SERVICE_ENABLED)
            .createdBy(UPDATED_CREATED_BY)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedBy(UPDATED_LAST_MODIFIED_BY);

        restMemberSettingsMockMvc.perform(put("/settings/api/member-settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMemberSettings)))
            .andExpect(status().isOk());

        // Validate the MemberSettings in the database
        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeUpdate);
        MemberSettings testMemberSettings = memberSettingsList.get(memberSettingsList.size() - 1);
        assertThat(testMemberSettings.getClientId()).isEqualTo(UPDATED_CLIENT_ID);
        assertThat(testMemberSettings.getSalesforceId()).isEqualTo(UPDATED_SALESFORCE_ID);
        assertThat(testMemberSettings.getParentSalesforceId()).isEqualTo(UPDATED_PARENT_SALESFORCE_ID);
        assertThat(testMemberSettings.isAssertionServiceEnabled()).isEqualTo(UPDATED_ASSERTION_SERVICE_ENABLED);
        assertThat(testMemberSettings.getCreatedBy()).isEqualTo(UPDATED_CREATED_BY);
        assertThat(testMemberSettings.getCreatedDate()).isEqualTo(UPDATED_CREATED_DATE);
        assertThat(testMemberSettings.getLastModifiedBy()).isEqualTo(UPDATED_LAST_MODIFIED_BY);
        assertThat(testMemberSettings.getLastModifiedDate()).isAfter(initialLastModified);
    }
    
    @Test
    public void updateNonExistingMemberSettings() throws Exception {
        int databaseSizeBeforeUpdate = memberSettingsRepository.findAll().size();

        // Create the MemberSettings

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMemberSettingsMockMvc.perform(put("/settings/api/member-settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(memberSettings)))
            .andExpect(status().isBadRequest());

        // Validate the MemberSettings in the database
        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    public void deleteMemberSettings() throws Exception {
        // Initialize the database
        memberSettingsRepository.save(memberSettings);

        int databaseSizeBeforeDelete = memberSettingsRepository.findAll().size();

        // Delete the memberSettings
        restMemberSettingsMockMvc.perform(delete("/settings/api/member-settings/{id}", memberSettings.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<MemberSettings> memberSettingsList = memberSettingsRepository.findAll();
        assertThat(memberSettingsList).hasSize(databaseSizeBeforeDelete - 1);
    }
    
    @Test
    public void equalsVerifier() throws Exception {        
        MemberSettings memberSettings1 = new MemberSettings();
        memberSettings1.setId("id1");
        assertThat(memberSettings1).isEqualTo(memberSettings1);
        MemberSettings memberSettings2 = new MemberSettings();
        memberSettings2.setId(memberSettings1.getId());
        assertThat(memberSettings1).isEqualTo(memberSettings2);
        memberSettings2.setId("id2");
        assertThat(memberSettings1).isNotEqualTo(memberSettings2);
        memberSettings1.setId(null);
        assertThat(memberSettings1).isNotEqualTo(memberSettings2);           
    }
}
