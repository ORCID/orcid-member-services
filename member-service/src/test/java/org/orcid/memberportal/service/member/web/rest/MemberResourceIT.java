package org.orcid.memberportal.service.member.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.orcid.memberportal.service.member.MemberServiceApp;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.repository.MemberRepository;
import org.orcid.memberportal.service.member.services.pojo.MemberServiceUser;
import org.orcid.memberportal.service.member.services.MemberService;
import org.orcid.memberportal.service.member.services.UserService;
import org.orcid.memberportal.service.member.web.rest.errors.ExceptionTranslator;
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

@SpringBootTest(classes = MemberServiceApp.class)
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
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private MemberService memberService;

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
            .setControllerAdvice(exceptionTranslator).setMessageConverters(jacksonMessageConverter).build();
        Mockito.when(mockedUserService.getLoggedInUser()).thenReturn(getLoggedInUser());
        ReflectionTestUtils.setField(memberService, "userService", mockedUserService);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getMembers() throws Exception {
        MvcResult result = restUserMockMvc
            .perform(get("/api/members").param("size", "50").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk()).andReturn();

        List<Member> members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(31);

        result = restUserMockMvc.perform(get("/api/members").param("size", "50").param("filter", "salesforceId").accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(31);

        result = restUserMockMvc.perform(get("/api/members").param("size", "50").param("filter", "salesforceId 4").accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(1);

        result = restUserMockMvc.perform(
                get("/api/members").param("size", "50").param("filter", "client 12").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(1);

        result = restUserMockMvc.perform(get("/api/members").param("size", "50").param("filter", "parent").accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(31);

        result = restUserMockMvc.perform(get("/api/members").param("size", "50").param("filter", "parent 1").accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(11); // parent 1, parent 10 - 19

        result = restUserMockMvc.perform(get("/api/members").param("size", "50").param("filter", "salesforceId+%2Btest").accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(1);

        result = restUserMockMvc.perform(get("/api/members").param("size", "50").param("filter", "client+%2Btest").accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(1);

        result = restUserMockMvc.perform(get("/api/members").param("size", "50").param("filter", "parent+%2Btest").accept(TestUtil.APPLICATION_JSON_UTF8)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)).andExpect(status().isOk()).andReturn();
        members = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<Member>>() {
        });
        assertThat(members.size()).isEqualTo(1);

    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getMembers_missingRoleAdmin() throws Exception {
        MvcResult result = restUserMockMvc
            .perform(get("/api/members").param("size", "50").accept(TestUtil.APPLICATION_JSON_UTF8).contentType(TestUtil.APPLICATION_JSON_UTF8))
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

    private MemberServiceUser getLoggedInUser() {
        MemberServiceUser user = new MemberServiceUser();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setSalesforceId(LOGGED_IN_SALESFORCE_ID);
        return user;
    }

}
