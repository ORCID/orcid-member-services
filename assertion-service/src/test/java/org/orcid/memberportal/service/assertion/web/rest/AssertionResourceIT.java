package org.orcid.memberportal.service.assertion.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.orcid.memberportal.service.assertion.AssertionServiceApp;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.services.AssertionService;
import org.orcid.memberportal.service.assertion.services.MemberService;
import org.orcid.memberportal.service.assertion.services.UserService;
import org.orcid.memberportal.service.assertion.web.rest.errors.ExceptionTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest(classes = AssertionServiceApp.class)
public class AssertionResourceIT {

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";
    private static final String OTHER_SALESFORCE_ID = "other-salesforce-id";
    private static final String LOGGED_IN_PASSWORD = "0123456789";
    private static final String LOGGED_IN_EMAIL = "loggedin@orcid.org";
    private static final String DEFAULT_LANGKEY = "en";

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private AssertionResource assertionResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private AssertionService assertionService;

    @Mock
    private UserService mockedUserService;
    
    @Mock
    private MemberService mockedMemberService;

    private MockMvc restUserMockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        createAssertions(DEFAULT_SALESFORCE_ID, 30);
        createAssertions(OTHER_SALESFORCE_ID, 70);
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(assertionResource).setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator).setMessageConverters(jacksonMessageConverter).build();
        Mockito.when(mockedUserService.getLoggedInUser()).thenReturn(getLoggedInUser());
        Mockito.when(mockedUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
        Mockito.when(mockedMemberService.getMemberDefaultLanguage(Mockito.anyString())).thenReturn("en");
        ReflectionTestUtils.setField(assertionService, "assertionsUserService", mockedUserService);
        ReflectionTestUtils.setField(assertionResource, "userService", mockedUserService);
        ReflectionTestUtils.setField(assertionResource, "memberService", mockedMemberService);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void testGetAssertions() throws Exception {
        // test only the 30 assertions for default salesforce id come back
        MvcResult result = restUserMockMvc
                .perform(get("/api/assertions").param("size", "50").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andReturn();
        List<Assertion> assertions = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Assertion>>() {
        });
        assertThat(assertions.size()).isEqualTo(30);

        result = restUserMockMvc.perform(get("/api/assertions").param("size", "50").param("filter", "department").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        assertions = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Assertion>>() {
        });
        assertThat(assertions.size()).isEqualTo(30);

        result = restUserMockMvc.perform(get("/api/assertions").param("size", "50").param("filter", "department 4").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        assertions = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Assertion>>() {
        });
        assertThat(assertions.size()).isEqualTo(1);

        result = restUserMockMvc.perform(
                get("/api/assertions").param("size", "50").param("filter", "org 12").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andReturn();
        assertions = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Assertion>>() {
        });
        assertThat(assertions.size()).isEqualTo(1);

        result = restUserMockMvc.perform(get("/api/assertions").param("size", "50").param("filter", "@orcid.org").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        assertions = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Assertion>>() {
        });
        assertThat(assertions.size()).isEqualTo(30);

        result = restUserMockMvc.perform(get("/api/assertions").param("size", "50").param("filter", "1@orcid.org").accept(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        assertions = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Assertion>>() {
        });
        assertThat(assertions.size()).isEqualTo(3); // 1@orcid.org, 11@orcid.org, 21@orcid.org

    }
    
    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void testSendNotifications() throws Exception {
        restUserMockMvc.perform(post("/api/assertion/notification-request").content("{ \"language\":\"en\" }").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
        List<Assertion> updatedAssertions = assertionRepository.findBySalesforceId(DEFAULT_SALESFORCE_ID);
        updatedAssertions.forEach(a -> assertThat(a.getStatus()).isEqualTo(AssertionStatus.NOTIFICATION_REQUESTED.name()));
    }
    
    private void createAssertions(String salesforceId, int quantity) {
        for (int i = 0; i < quantity; i++) {
            assertionRepository.save(getAssertion(String.valueOf(i), salesforceId));
        }
    }

    private Assertion getAssertion(String identifier, String salesforceId) {
        Assertion assertion = new Assertion();
        assertion.setEmail(identifier + "@orcid.org");
        assertion.setSalesforceId(salesforceId);
        assertion.setAffiliationSection(AffiliationSection.EMPLOYMENT);
        assertion.setDepartmentName("department " + identifier);
        assertion.setOrgCity("city " + identifier);
        assertion.setOrgName("org " + identifier);
        assertion.setOrgRegion("region " + identifier);
        assertion.setOrgCountry("us " + identifier);
        assertion.setDisambiguatedOrgId("id " + identifier);
        assertion.setDisambiguationSource("source " + identifier);
        assertion.setStatus(AssertionStatus.PENDING.name());
        return assertion;
    }

    private AssertionServiceUser getLoggedInUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(DEFAULT_SALESFORCE_ID);
        return user;
    }

}
