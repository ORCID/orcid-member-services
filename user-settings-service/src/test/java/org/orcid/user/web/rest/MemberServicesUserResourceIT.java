package org.orcid.user.web.rest;

import org.orcid.user.UserSettingsServiceApp;
import org.orcid.user.config.SecurityBeanOverrideConfiguration;
import org.orcid.user.domain.MemberServicesUser;
import org.orcid.user.repository.MemberServicesUserRepository;
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

import java.util.List;

import static org.orcid.user.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@Link MemberServicesUserResource} REST controller.
 */
@SpringBootTest(classes = { SecurityBeanOverrideConfiguration.class, UserSettingsServiceApp.class })
public class MemberServicesUserResourceIT {

	private static final String DEFAULT_SALESFORCE_ID = "AAAAAAAAAA";
	private static final String UPDATED_SALESFORCE_ID = "BBBBBBBBBB";

	private static final String DEFAULT_PARENT_SALESFORCE_ID = "AAAAAAAAAA";
	private static final String UPDATED_PARENT_SALESFORCE_ID = "BBBBBBBBBB";

	private static final Boolean DEFAULT_DISABLED = false;
	private static final Boolean UPDATED_DISABLED = true;

	private static final Boolean DEFAULT_MAIN_CONTACT = false;
	private static final Boolean UPDATED_MAIN_CONTACT = true;

	private static final Boolean DEFAULT_ASSERTION_SERVICE_ENABLED = false;
	private static final Boolean UPDATED_ASSERTION_SERVICE_ENABLED = true;

	@Autowired
	private MemberServicesUserRepository memberServicesUserRepository;

	@Autowired
	private MappingJackson2HttpMessageConverter jacksonMessageConverter;

	@Autowired
	private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

	@Autowired
	private ExceptionTranslator exceptionTranslator;

	@Autowired
	private Validator validator;

	private MockMvc restMemberServicesUserMockMvc;

	private MemberServicesUser memberServicesUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
		final MemberServicesUserResource memberServicesUserResource = new MemberServicesUserResource(
				memberServicesUserRepository);
		this.restMemberServicesUserMockMvc = MockMvcBuilders.standaloneSetup(memberServicesUserResource)
				.setCustomArgumentResolvers(pageableArgumentResolver).setControllerAdvice(exceptionTranslator)
				.setConversionService(createFormattingConversionService()).setMessageConverters(jacksonMessageConverter)
				.setValidator(validator).build();
	}

	/**
	 * Create an entity for this test.
	 *
	 * This is a static method, as tests for other entities might also need it,
	 * if they test an entity which requires the current entity.
	 */
	public static MemberServicesUser createEntity() {
		MemberServicesUser memberServicesUser = new MemberServicesUser();
		memberServicesUser.setSalesforceId(DEFAULT_SALESFORCE_ID);
		memberServicesUser.setParentSalesforceId(DEFAULT_PARENT_SALESFORCE_ID);
		memberServicesUser.setDisabled(DEFAULT_DISABLED);
		memberServicesUser.setMainContact(DEFAULT_MAIN_CONTACT);
		memberServicesUser.setAssertionServiceEnabled(DEFAULT_ASSERTION_SERVICE_ENABLED);
		return memberServicesUser;
	}

	/**
	 * Create an updated entity for this test.
	 *
	 * This is a static method, as tests for other entities might also need it,
	 * if they test an entity which requires the current entity.
	 */
	public static MemberServicesUser createUpdatedEntity() {
		MemberServicesUser memberServicesUser = new MemberServicesUser();
		memberServicesUser.setSalesforceId(UPDATED_SALESFORCE_ID);
		memberServicesUser.setParentSalesforceId(UPDATED_PARENT_SALESFORCE_ID);
		memberServicesUser.setDisabled(UPDATED_DISABLED);
		memberServicesUser.setMainContact(UPDATED_MAIN_CONTACT);
		memberServicesUser.setAssertionServiceEnabled(UPDATED_ASSERTION_SERVICE_ENABLED);
		return memberServicesUser;
	}

	@BeforeEach
	public void initTest() {
		memberServicesUserRepository.deleteAll();
		memberServicesUser = createEntity();
	}

	@Test
	public void createMemberServicesUser() throws Exception {
		int databaseSizeBeforeCreate = memberServicesUserRepository.findAll().size();

		// Create the MemberServicesUser
		restMemberServicesUserMockMvc
				.perform(post("/api/member-services-users").contentType(TestUtil.APPLICATION_JSON_UTF8)
						.content(TestUtil.convertObjectToJsonBytes(memberServicesUser)))
				.andExpect(status().isCreated());

		// Validate the MemberServicesUser in the database
		List<MemberServicesUser> memberServicesUserList = memberServicesUserRepository.findAll();
		assertThat(memberServicesUserList).hasSize(databaseSizeBeforeCreate + 1);
		MemberServicesUser testMemberServicesUser = memberServicesUserList.get(memberServicesUserList.size() - 1);
		assertThat(testMemberServicesUser.getSalesforceId()).isEqualTo(DEFAULT_SALESFORCE_ID);
		assertThat(testMemberServicesUser.getParentSalesforceId()).isEqualTo(DEFAULT_PARENT_SALESFORCE_ID);
		assertThat(testMemberServicesUser.isDisabled()).isEqualTo(DEFAULT_DISABLED);
		assertThat(testMemberServicesUser.isMainContact()).isEqualTo(DEFAULT_MAIN_CONTACT);
		assertThat(testMemberServicesUser.isAssertionServiceEnabled()).isEqualTo(DEFAULT_ASSERTION_SERVICE_ENABLED);
	}

	@Test
	public void createMemberServicesUserWithExistingId() throws Exception {
		int databaseSizeBeforeCreate = memberServicesUserRepository.findAll().size();

		// Create the MemberServicesUser with an existing ID
		memberServicesUser.setId("existing_id");

		// An entity with an existing ID cannot be created, so this API call
		// must fail
		restMemberServicesUserMockMvc
				.perform(post("/api/member-services-users").contentType(TestUtil.APPLICATION_JSON_UTF8)
						.content(TestUtil.convertObjectToJsonBytes(memberServicesUser)))
				.andExpect(status().isBadRequest());

		// Validate the MemberServicesUser in the database
		List<MemberServicesUser> memberServicesUserList = memberServicesUserRepository.findAll();
		assertThat(memberServicesUserList).hasSize(databaseSizeBeforeCreate);
	}

	@Test
	public void getAllMemberServicesUsers() throws Exception {
		// Initialize the database
		memberServicesUserRepository.save(memberServicesUser);

		// Get all the memberServicesUserList
		restMemberServicesUserMockMvc.perform(get("/api/member-services-users?sort=id,desc")).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andExpect(jsonPath("$.[*].id").value(hasItem(memberServicesUser.getId())))				
				.andExpect(jsonPath("$.[*].salesforceId").value(hasItem(DEFAULT_SALESFORCE_ID.toString())))
				.andExpect(jsonPath("$.[*].parentSalesforceId").value(hasItem(DEFAULT_PARENT_SALESFORCE_ID.toString())))
				.andExpect(jsonPath("$.[*].disabled").value(hasItem(DEFAULT_DISABLED.booleanValue())))
				.andExpect(jsonPath("$.[*].mainContact").value(hasItem(DEFAULT_MAIN_CONTACT.booleanValue())))
				.andExpect(jsonPath("$.[*].assertionServiceEnabled")
						.value(hasItem(DEFAULT_ASSERTION_SERVICE_ENABLED.booleanValue())));
	}

	@Test
	public void getMemberServicesUser() throws Exception {
		// Initialize the database
		memberServicesUserRepository.save(memberServicesUser);

		// Get the memberServicesUser
		restMemberServicesUserMockMvc.perform(get("/api/member-services-users/{id}", memberServicesUser.getId()))
				.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andExpect(jsonPath("$.id").value(memberServicesUser.getId()))				
				.andExpect(jsonPath("$.salesforceId").value(DEFAULT_SALESFORCE_ID.toString()))
				.andExpect(jsonPath("$.parentSalesforceId").value(DEFAULT_PARENT_SALESFORCE_ID.toString()))
				.andExpect(jsonPath("$.disabled").value(DEFAULT_DISABLED.booleanValue()))
				.andExpect(jsonPath("$.mainContact").value(DEFAULT_MAIN_CONTACT.booleanValue())).andExpect(
						jsonPath("$.assertionServiceEnabled").value(DEFAULT_ASSERTION_SERVICE_ENABLED.booleanValue()));
	}

	@Test
	public void getNonExistingMemberServicesUser() throws Exception {
		// Get the memberServicesUser
		restMemberServicesUserMockMvc.perform(get("/api/member-services-users/{id}", Long.MAX_VALUE))
				.andExpect(status().isNotFound());
	}

	@Test
	public void updateMemberServicesUser() throws Exception {
		// Initialize the database
		memberServicesUserRepository.save(memberServicesUser);

		int databaseSizeBeforeUpdate = memberServicesUserRepository.findAll().size();

		// Update the memberServicesUser
		MemberServicesUser updatedMemberServicesUser = memberServicesUserRepository.findById(memberServicesUser.getId())
				.get();
		updatedMemberServicesUser.setSalesforceId(UPDATED_SALESFORCE_ID);
		updatedMemberServicesUser.setParentSalesforceId(UPDATED_PARENT_SALESFORCE_ID);
		updatedMemberServicesUser.setDisabled(UPDATED_DISABLED);
		updatedMemberServicesUser.setMainContact(UPDATED_MAIN_CONTACT);
		updatedMemberServicesUser.setAssertionServiceEnabled(UPDATED_ASSERTION_SERVICE_ENABLED);

		restMemberServicesUserMockMvc
				.perform(put("/api/member-services-users").contentType(TestUtil.APPLICATION_JSON_UTF8)
						.content(TestUtil.convertObjectToJsonBytes(updatedMemberServicesUser)))
				.andExpect(status().isOk());

		// Validate the MemberServicesUser in the database
		List<MemberServicesUser> memberServicesUserList = memberServicesUserRepository.findAll();
		assertThat(memberServicesUserList).hasSize(databaseSizeBeforeUpdate);
		MemberServicesUser testMemberServicesUser = memberServicesUserList.get(memberServicesUserList.size() - 1);
		assertThat(testMemberServicesUser.getSalesforceId()).isEqualTo(UPDATED_SALESFORCE_ID);
		assertThat(testMemberServicesUser.getParentSalesforceId()).isEqualTo(UPDATED_PARENT_SALESFORCE_ID);
		assertThat(testMemberServicesUser.isDisabled()).isEqualTo(UPDATED_DISABLED);
		assertThat(testMemberServicesUser.isMainContact()).isEqualTo(UPDATED_MAIN_CONTACT);
		assertThat(testMemberServicesUser.isAssertionServiceEnabled()).isEqualTo(UPDATED_ASSERTION_SERVICE_ENABLED);
	}

	@Test
	public void updateNonExistingMemberServicesUser() throws Exception {
		int databaseSizeBeforeUpdate = memberServicesUserRepository.findAll().size();

		// Create the MemberServicesUser

		// If the entity doesn't have an ID, it will throw
		// BadRequestAlertException
		restMemberServicesUserMockMvc
				.perform(put("/api/member-services-users").contentType(TestUtil.APPLICATION_JSON_UTF8)
						.content(TestUtil.convertObjectToJsonBytes(memberServicesUser)))
				.andExpect(status().isBadRequest());

		// Validate the MemberServicesUser in the database
		List<MemberServicesUser> memberServicesUserList = memberServicesUserRepository.findAll();
		assertThat(memberServicesUserList).hasSize(databaseSizeBeforeUpdate);
	}

	@Test
	public void deleteMemberServicesUser() throws Exception {
		// Initialize the database
		memberServicesUserRepository.save(memberServicesUser);

		int databaseSizeBeforeDelete = memberServicesUserRepository.findAll().size();

		// Delete the memberServicesUser
		restMemberServicesUserMockMvc.perform(delete("/api/member-services-users/{id}", memberServicesUser.getId())
				.accept(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isNoContent());

		// Validate the database contains one less item
		List<MemberServicesUser> memberServicesUserList = memberServicesUserRepository.findAll();
		assertThat(memberServicesUserList).hasSize(databaseSizeBeforeDelete - 1);
	}

	@Test
	public void equalsVerifier() throws Exception {
		TestUtil.equalsVerifier(MemberServicesUser.class);
		MemberServicesUser memberServicesUser1 = new MemberServicesUser();
		memberServicesUser1.setId("id1");
		MemberServicesUser memberServicesUser2 = new MemberServicesUser();
		memberServicesUser2.setId(memberServicesUser1.getId());
		assertThat(memberServicesUser1).isEqualTo(memberServicesUser2);
		memberServicesUser2.setId("id2");
		assertThat(memberServicesUser1).isNotEqualTo(memberServicesUser2);
		memberServicesUser1.setId(null);
		assertThat(memberServicesUser1).isNotEqualTo(memberServicesUser2);
	}
}
