package org.orcid.mp.member.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.orcid.mp.member.MemberServiceApplication;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.SimpleExceptionHandler;
import org.orcid.mp.member.service.ApiClientDetailsService;
import org.orcid.mp.member.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MemberServiceApplication.class)
public class ApiCredentialsResourceIT {

    private static final String LOGGED_IN_PASSWORD = "0123456789";
    private static final String LOGGED_IN_EMAIL = "admin@orcid.org";
    private static final String LOGGED_IN_MEMBER_ID = "memberId";

    @Autowired
    private ApiCredentialsResource apiCredentialsResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private SimpleExceptionHandler simpleExceptionHandler;

    @Mock
    private UserService mockedUserService;

    @Mock
    private ApiClientDetailsService mockedApiClientDetailsService;

    private MockMvc restMockMvc;

    @BeforeEach
    public void setup() {
        this.restMockMvc = MockMvcBuilders.standaloneSetup(apiCredentialsResource)
                .setControllerAdvice(simpleExceptionHandler)
                .setMessageConverters(jacksonMessageConverter)
                .build();

        // Inject the mocked services into the resource manually
        ReflectionTestUtils.setField(apiCredentialsResource, "userService", mockedUserService);
        ReflectionTestUtils.setField(apiCredentialsResource, "apiClientDetailsService", mockedApiClientDetailsService);
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getApiClientDetails_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Mockito.when(mockedUserService.getLoggedInUser()).thenReturn(getLoggedInAdminUser());

        String clientId = "APP-12345ABCDE";
        ApiClientDetails mockDetails = new ApiClientDetails();
        mockDetails.setClientDetailsId(clientId);
        mockDetails.setMemberId("MEMBER-1");
        mockDetails.setClientName("Integration Test Client");

        Mockito.when(mockedApiClientDetailsService.getApiClientDetails(clientId)).thenReturn(mockDetails);

        restMockMvc.perform(get("/apicreds/{clientDetailsId}", clientId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientDetailsId").value(clientId))
                .andExpect(jsonPath("$.memberId").value("MEMBER-1"))
                .andExpect(jsonPath("$.clientName").value("Integration Test Client"));
    }

    @Test
    @WithMockUser(username = "user@orcid.org", authorities = { "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getApiClientDetails_WhenUserDoesNotBelongToMember_ShouldReturnForbidden() throws Exception {
        Mockito.when(mockedUserService.getLoggedInUser()).thenReturn(getLoggedInNonAdminUser("MEMBER-OTHER"));

        String clientId = "APP-12345ABCDE";
        ApiClientDetails mockDetails = new ApiClientDetails();
        mockDetails.setClientDetailsId(clientId);
        mockDetails.setMemberId("MEMBER-TARGET");

        Mockito.when(mockedApiClientDetailsService.getApiClientDetails(clientId)).thenReturn(mockDetails);

        restMockMvc.perform(get("/apicreds/{clientDetailsId}", clientId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // AccessDeniedException mapped to 403 by SimpleExceptionHandler
    }

    @Test
    @WithMockUser(username = LOGGED_IN_EMAIL, authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getClientsForMember_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Mockito.when(mockedUserService.getLoggedInUser()).thenReturn(getLoggedInAdminUser());

        String memberId = "MEMBER-1";

        ApiClientDetails summary = new ApiClientDetails();
        summary.setClientDetailsId("APP-999");
        summary.setClientName("Summary Client");

        ApiClientSummaryPage mockPage = new ApiClientSummaryPage();
        mockPage.setContent(Collections.singletonList(summary));
        mockPage.setTotalElements(1);

        Mockito.when(mockedApiClientDetailsService.getApiClientsForMember(memberId)).thenReturn(mockPage);

        restMockMvc.perform(get("/apicreds/member/{memberId}", memberId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].clientDetailsId").value("APP-999"))
                .andExpect(jsonPath("$.content[0].clientName").value("Summary Client"));
    }

    @Test
    @WithMockUser(username = "user@orcid.org", authorities = { "ROLE_USER" }, password = LOGGED_IN_PASSWORD)
    public void getClientsForMember_WhenUserDoesNotBelongToMember_ShouldReturnForbidden() throws Exception {
        Mockito.when(mockedUserService.getLoggedInUser()).thenReturn(getLoggedInNonAdminUser("MEMBER-OTHER"));

        String memberId = "MEMBER-TARGET";

        ApiClientSummaryPage mockPage = new ApiClientSummaryPage();
        Mockito.when(mockedApiClientDetailsService.getApiClientsForMember(memberId)).thenReturn(mockPage);

        restMockMvc.perform(get("/apicreds/member/{memberId}", memberId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // AccessDeniedException mapped to 403 by SimpleExceptionHandler
    }

    private User getLoggedInAdminUser() {
        User user = new User();
        user.setEmail(LOGGED_IN_EMAIL);
        user.setMemberId(LOGGED_IN_MEMBER_ID);
        user.setAdmin(true);
        return user;
    }

    private User getLoggedInNonAdminUser(String memberId) {
        User user = new User();
        user.setEmail("user@orcid.org");
        user.setMemberId(memberId);
        user.setAdmin(false);
        return user;
    }
}