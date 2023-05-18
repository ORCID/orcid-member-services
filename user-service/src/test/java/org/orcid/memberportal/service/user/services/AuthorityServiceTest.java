package org.orcid.memberportal.service.user.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.orcid.memberportal.service.user.security.MockSecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorityServiceTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private AuthorityService authorityService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SecurityContextHolder.setContext(new MockSecurityContext("username"));
    }

    @Test
    void testGetAuthoritiesForUser_assertionsEnabledConsortiumLeadOrgOwnerAdminEnabled() {
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsConsortiumLead(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsAdminEnabled(Mockito.eq("salesforce-id"))).thenReturn(true);

        User user = new User();
        user.setSalesforceId("salesforce-id");
        user.setAdmin(true);
        user.setMainContact(true);

        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        assertThat(authorities).isNotNull();
        assertThat(authorities.size()).isEqualTo(5);
        assertThat(authorities.contains(AuthoritiesConstants.USER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ADMIN)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ORG_OWNER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.CONSORTIUM_LEAD)).isTrue();

        Mockito.verify(memberService).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsConsortiumLead(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsAdminEnabled(Mockito.eq("salesforce-id"));
    }

    @Test
    void testGetAuthoritiesForUser_assertionsEnabledConsortiumLeadOrgOwnerAdminNotEnabledOnMember() {
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsConsortiumLead(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsAdminEnabled(Mockito.eq("salesforce-id"))).thenReturn(false);

        User user = new User();
        user.setSalesforceId("salesforce-id");
        user.setAdmin(true);
        user.setMainContact(true);

        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        assertThat(authorities).isNotNull();
        assertThat(authorities.size()).isEqualTo(4);
        assertThat(authorities.contains(AuthoritiesConstants.USER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ORG_OWNER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.CONSORTIUM_LEAD)).isTrue();

        Mockito.verify(memberService).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsConsortiumLead(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsAdminEnabled(Mockito.eq("salesforce-id"));
    }

    @Test
    void testGetAuthoritiesForUser_assertionsEnabledConsortiumLeadOrgOwnerAdminDisabledOnUser() {
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsConsortiumLead(Mockito.eq("salesforce-id"))).thenReturn(true);

        User user = new User();
        user.setSalesforceId("salesforce-id");
        user.setAdmin(false);
        user.setMainContact(true);

        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        assertThat(authorities).isNotNull();
        assertThat(authorities.size()).isEqualTo(4);
        assertThat(authorities.contains(AuthoritiesConstants.USER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ORG_OWNER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.CONSORTIUM_LEAD)).isTrue();

        Mockito.verify(memberService).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsConsortiumLead(Mockito.eq("salesforce-id"));
    }

    @Test
    void testGetAuthoritiesForUser_assertionsEnabledConsortiumLeadOrgOwnerAdminDisabledOnUserAndOrg() {
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsConsortiumLead(Mockito.eq("salesforce-id"))).thenReturn(true);

        User user = new User();
        user.setSalesforceId("salesforce-id");
        user.setAdmin(false);
        user.setMainContact(true);

        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        assertThat(authorities).isNotNull();
        assertThat(authorities.size()).isEqualTo(4);
        assertThat(authorities.contains(AuthoritiesConstants.USER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ORG_OWNER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.CONSORTIUM_LEAD)).isTrue();

        Mockito.verify(memberService).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsConsortiumLead(Mockito.eq("salesforce-id"));
    }

    @Test
    void testGetAuthoritiesForUser_assertionsEnabledConsortiumLeadNotOrgOwner() {
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsConsortiumLead(Mockito.eq("salesforce-id"))).thenReturn(true);

        User user = new User();
        user.setSalesforceId("salesforce-id");
        user.setAdmin(false);
        user.setMainContact(false);

        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        assertThat(authorities).isNotNull();
        assertThat(authorities.size()).isEqualTo(3);
        assertThat(authorities.contains(AuthoritiesConstants.USER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.CONSORTIUM_LEAD)).isTrue();

        Mockito.verify(memberService).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsConsortiumLead(Mockito.eq("salesforce-id"));
    }

    @Test
    void testGetAuthoritiesForUser_assertionsEnabledNotConsortiumLead() {
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"))).thenReturn(true);
        Mockito.when(memberService.memberIsConsortiumLead(Mockito.eq("salesforce-id"))).thenReturn(false);

        User user = new User();
        user.setSalesforceId("salesforce-id");
        user.setAdmin(false);
        user.setMainContact(false);

        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        assertThat(authorities).isNotNull();
        assertThat(authorities.size()).isEqualTo(2);
        assertThat(authorities.contains(AuthoritiesConstants.USER)).isTrue();
        assertThat(authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)).isTrue();

        Mockito.verify(memberService).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsConsortiumLead(Mockito.eq("salesforce-id"));
    }


    @Test
    void testGetAuthoritiesForUser_noAssertionsEnabled() {
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"))).thenReturn(false);
        Mockito.when(memberService.memberIsConsortiumLead(Mockito.eq("salesforce-id"))).thenReturn(false);

        User user = new User();
        user.setSalesforceId("salesforce-id");
        user.setAdmin(false);
        user.setMainContact(false);

        Set<String> authorities = authorityService.getAuthoritiesForUser(user);
        assertThat(authorities).isNotNull();
        assertThat(authorities.size()).isEqualTo(1);
        assertThat(authorities.contains(AuthoritiesConstants.USER)).isTrue();

        Mockito.verify(memberService).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.eq("salesforce-id"));
        Mockito.verify(memberService).memberIsConsortiumLead(Mockito.eq("salesforce-id"));
    }
}
