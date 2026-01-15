package org.orcid.mp.member.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.orcid.mp.member.MemberServiceApplication;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.SimpleExceptionHandler;
import org.orcid.mp.member.repository.MemberRepository;
import org.orcid.mp.member.service.MemberService;
import org.orcid.mp.member.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MemberServiceApplication.class)
public class MemberResourceIT {

    private static final String LOGGED_IN_PASSWORD = "0123456789";
    private static final String LOGGED_IN_EMAIL = "loggedin@orcid.org";
    private static final String LOGGED_IN_SALESFORCE_ID = "salesforceId";
    private static final String DEFAULT_LANGKEY = "en";

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberResource memberResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SimpleExceptionHandler simpleExceptionHandler;

    @Mock
    private UserService mockedUserService;

    private MockMvc restUserMockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        memberRepository.deleteAll();
        createMembers(30);
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(memberResource).setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(simpleExceptionHandler).setMessageConverters(jacksonMessageConverter).build();
        Mockito.when(mockedUserService.getLoggedInUser()).thenReturn(getLoggedInUser());
        ReflectionTestUtils.setField(memberService, "userService", mockedUserService);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getMembers() throws Exception {
        MvcResult result = restUserMockMvc
                .perform(get("/members").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(31)).andReturn();

        result = restUserMockMvc.perform(get("/members").param("size", "50").param("filter", "salesforceId").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(31)).andReturn();

        result = restUserMockMvc.perform(get("/members").param("size", "50").param("filter", "salesforceId 4").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(
                        get("/members").param("size", "50").param("filter", "client 12").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(get("/members").param("size", "50").param("filter", "parent").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(31)).andReturn();

        result = restUserMockMvc.perform(get("/members").param("size", "50").param("filter", "parent 1").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(11)).andReturn();

        result = restUserMockMvc.perform(get("/members").param("size", "50").param("filter", "salesforceId+%2Btest").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(get("/members").param("size", "50").param("filter", "client+%2Btest").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();

        result = restUserMockMvc.perform(get("/members").param("size", "50").param("filter", "parent+%2Btest").accept(RestTestUtil.APPLICATION_JSON_UTF8)
                .contentType(RestTestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andReturn();
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getMembers_missingRoleAdmin() throws Exception {
        MvcResult result = restUserMockMvc
                .perform(get("/members").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isForbidden()).andReturn();
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getMembersList() throws Exception {
        MvcResult result = restUserMockMvc
                .perform(get("/members/list/all").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getMembersList_missingRoleAdmin() throws Exception {
        MvcResult result = restUserMockMvc
                .perform(get("/members/list/all").param("size", "50").accept(RestTestUtil.APPLICATION_JSON_UTF8).contentType(RestTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isForbidden()).andReturn();
    }


    private void createMembers(int quantity) {
        for (int i = 0; i < quantity; i++) {
            memberRepository.save(getMember(String.valueOf(i)));
        }
        memberRepository.save(getMember("+test"));
    }

    private Member getMember(String identifier) {
        Member member = new Member();
        member.setSalesforceId("salesforceId " + identifier);
        member.setClientName("client " + identifier);
        member.setParentSalesforceId("parent " + identifier);
        member.setAssertionServiceEnabled(true);
        member.setIsConsortiumLead(false);
        return member;
    }

    private User getLoggedInUser() {
        User user = new User();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(LOGGED_IN_SALESFORCE_ID);
        return user;
    }

}
