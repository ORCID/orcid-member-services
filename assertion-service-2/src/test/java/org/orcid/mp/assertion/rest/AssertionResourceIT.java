package org.orcid.mp.assertion.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.orcid.mp.assertion.AssertionServiceApplication;
import org.orcid.mp.assertion.client.MemberServiceClient;
import org.orcid.mp.assertion.client.UserServiceClient;
import org.orcid.mp.assertion.domain.*;
import org.orcid.mp.assertion.error.SimpleExceptionHandler;
import org.orcid.mp.assertion.repository.AssertionRepository;
import org.orcid.mp.assertion.service.AssertionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AssertionServiceApplication.class)
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
    private AssertionService assertionService;

    @Autowired
    private SimpleExceptionHandler simpleExceptionHandler;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MemberServiceClient memberServiceClient;

    private MockMvc restUserMockMvc;

    @BeforeEach
    public void setup() {
        assertionRepository.deleteAll();
        createAssertions(DEFAULT_SALESFORCE_ID, 30);
        createAssertions(OTHER_SALESFORCE_ID, 70);
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(assertionResource).setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(simpleExceptionHandler).setMessageConverters(jacksonMessageConverter).build();
        Mockito.when(userServiceClient.getUser(anyString())).thenReturn(getLoggedInUser());
        Mockito.when(memberServiceClient.getMember(anyString())).thenReturn(getMember());
        ReflectionTestUtils.setField(assertionService, "userServiceClient", userServiceClient);
        ReflectionTestUtils.setField(assertionResource, "userServiceClient", userServiceClient);
        ReflectionTestUtils.setField(assertionResource, "memberServiceClient", memberServiceClient);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void testGetAssertions() throws Exception {
        // test only the 30 assertions for default salesforce id come back
        MvcResult result = restUserMockMvc
                .perform(get("/assertions").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(30)).andReturn();

        result = restUserMockMvc.perform(get("/assertions").param("size", "50").param("filter", "department").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(30)).andReturn();

        result = restUserMockMvc.perform(get("/assertions").param("size", "50").param("filter", "department 4").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(
                        get("/assertions").param("size", "50").param("filter", "org 12").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(get("/assertions").param("size", "50").param("filter", "@orcid.org").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(30)).andReturn();

        result = restUserMockMvc.perform(get("/assertions").param("size", "50").param("filter", "1@orcid.org").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3)).andReturn();
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = LOGGED_IN_PASSWORD)
    public void testSendNotifications() throws Exception {
        restUserMockMvc.perform(post("/assertions/notification-request").content("{ \"language\":\"en\" }").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
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

    private User getLoggedInUser() {
        User user = new User();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(DEFAULT_SALESFORCE_ID);
        return user;
    }

    private Member getMember() {
        Member member = new Member();
        member.setDefaultLanguage("en");
        return member;
    }

}