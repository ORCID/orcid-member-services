package org.orcid.memberportal.service.member.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.member.config.ApplicationProperties;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.service.reports.ReportInfo;
import org.orcid.memberportal.service.member.service.user.MemberServiceUser;
import org.orcid.memberportal.service.member.web.rest.errors.BadRequestAlertException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

public class ReportServiceTest {
    
    private static final String CONSORTIA_DASHBOARD_URL = "https://secure.holistics.io/embed/consortia";
    
    private static final String INTEGRATION_DASHBOARD_URL = "https://secure.holistics.io/embed/integration";
    
    private static final String AFFILIATION_DASHBOARD_URL = "https://secure.holistics.io/embed/affiliation";
    
    private static final String MEMBER_DASHBOARD_URL = "https://secure.holistics.io/embed/member";

    private static final String CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_URL = "https://secure.holistics.io/embed/consortia-member-affiliations";
    
    private static final String CONSORTIA_DASHBOARD_SECRET = "some-long-holistics-consortia-dashboard-secret";
    
    private static final String INTEGRATION_DASHBOARD_SECRET = "some-long-holistics-integration-dashboard-secret";
    
    private static final String AFFILIATION_DASHBOARD_SECRET = "some-long-holistics-affiliation-dashboard-secret";
    
    private static final String MEMBER_DASHBOARD_SECRET = "some-long-holistics-member-dashboard-secret";
    
    private static final String CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_SECRET = "https://secure.holistics.io/embed/consortia-member-affiliations-secret";

    @Mock
    private ApplicationProperties mockApplicationProperties;

    @Mock
    private UserService mockUserService;

    @Mock
    private MemberService mockMemberService;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mockApplicationProperties.getHolisticsConsortiaDashboardUrl()).thenReturn(CONSORTIA_DASHBOARD_URL);
        Mockito.when(mockApplicationProperties.getHolisticsConsortiaDashboardSecret()).thenReturn(CONSORTIA_DASHBOARD_SECRET);
        Mockito.when(mockApplicationProperties.getHolisticsMemberDashboardUrl()).thenReturn(MEMBER_DASHBOARD_URL);
        Mockito.when(mockApplicationProperties.getHolisticsMemberDashboardSecret()).thenReturn(MEMBER_DASHBOARD_SECRET);
        Mockito.when(mockApplicationProperties.getHolisticsAffiliationDashboardUrl()).thenReturn(AFFILIATION_DASHBOARD_URL);
        Mockito.when(mockApplicationProperties.getHolisticsAffiliationDashboardSecret()).thenReturn(AFFILIATION_DASHBOARD_SECRET);
        Mockito.when(mockApplicationProperties.getHolisticsIntegrationDashboardUrl()).thenReturn(INTEGRATION_DASHBOARD_URL);
        Mockito.when(mockApplicationProperties.getHolisticsIntegrationDashboardSecret()).thenReturn(INTEGRATION_DASHBOARD_SECRET);
        Mockito.when(mockApplicationProperties.getHolisticsConsortiaMemberAffiliationsDashboardUrl()).thenReturn(CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_URL);
        Mockito.when(mockApplicationProperties.getHolisticsConsortiaMemberAffiliationsDashboardSecret()).thenReturn(CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_SECRET);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetMemberReportInfo() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("salesforce-id"))).thenReturn(Optional.of(getConsortiumLeadMember()));
        
        ReportInfo reportInfo = reportService.getMemberReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo(MEMBER_DASHBOARD_URL);
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();
        
        Assertions.assertThrows(SignatureException.class, () -> {
            checkCommonClaims(reportInfo.getJwt(), MEMBER_DASHBOARD_URL); // wrong secret
        });
        
        checkCommonClaims(reportInfo.getJwt(), MEMBER_DASHBOARD_SECRET);
        
        Claims claims = parseClaims(reportInfo.getJwt(), MEMBER_DASHBOARD_SECRET);
        assertThat(claims.get(ReportService.FILTERS_PARAM)).isNotNull();

        Map<String, Object> filter = (Map<String, Object>) claims.get(ReportService.FILTERS_PARAM);
        assertThat(filter.get(ReportService.MEMBER_NAME_FILTER)).isNotNull();
        
        Map<String, Object> memberNameFilter = (Map<String, Object>) filter.get(ReportService.MEMBER_NAME_FILTER);
        assertThat(memberNameFilter.get(ReportService.HIDDEN_PARAM)).isNotNull();
        assertThat((boolean) memberNameFilter.get(ReportService.HIDDEN_PARAM)).isTrue();

        Mockito.verify(mockApplicationProperties).getHolisticsMemberDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsMemberDashboardSecret();
        Mockito.verify(mockUserService).getLoggedInUser();
    }
    
    @Test
    public void testGetAffiliationReportInfo() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("salesforce-id"))).thenReturn(Optional.of(getConsortiumMember()));
        
        ReportInfo reportInfo = reportService.getAffiliationReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo(AFFILIATION_DASHBOARD_URL);
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();
        
        Assertions.assertThrows(SignatureException.class, () -> {
            checkCommonClaims(reportInfo.getJwt(), AFFILIATION_DASHBOARD_URL); // wrong secret
        });
        
        checkCommonClaims(reportInfo.getJwt(), AFFILIATION_DASHBOARD_SECRET);

        Mockito.verify(mockApplicationProperties).getHolisticsAffiliationDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsAffiliationDashboardSecret();
        Mockito.verify(mockUserService, Mockito.times(2)).getLoggedInUser();
    }

    @Test
    public void testGetIntegrationReportInfo() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("salesforce-id"))).thenReturn(Optional.of(getConsortiumLeadMember()));
        
        ReportInfo reportInfo = reportService.getIntegrationReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo(INTEGRATION_DASHBOARD_URL);
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();
        
        Assertions.assertThrows(SignatureException.class, () -> {
            checkCommonClaims(reportInfo.getJwt(), INTEGRATION_DASHBOARD_URL); // wrong secret
        });
        
        checkCommonClaims(reportInfo.getJwt(), INTEGRATION_DASHBOARD_SECRET);

        Mockito.verify(mockApplicationProperties).getHolisticsIntegrationDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsIntegrationDashboardSecret();
        Mockito.verify(mockUserService).getLoggedInUser();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetConsortiaReportInfo() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("salesforce-id"))).thenReturn(Optional.of(getConsortiumLeadMember()));
        
        ReportInfo reportInfo = reportService.getConsortiaReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo(CONSORTIA_DASHBOARD_URL);
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();
        
        Assertions.assertThrows(SignatureException.class, () -> {
            checkCommonClaims(reportInfo.getJwt(), CONSORTIA_DASHBOARD_URL); // wrong secret
        });
        
        checkCommonClaims(reportInfo.getJwt(), CONSORTIA_DASHBOARD_SECRET);

        Claims claims = parseClaims(reportInfo.getJwt(), CONSORTIA_DASHBOARD_SECRET);
        Map<String, Object> drillthroughs = (Map<String, Object>) claims.get(ReportService.DRILLTHROUGHS_PARAM);

        assertThat(drillthroughs).isNotNull();
        assertThat(drillthroughs.get(ReportService.CONSORTIA_DRILLTHROUGH_KEY)).isNotNull();
        
        Map<String, Object> consortiaDrillthrough = (Map<String, Object>) drillthroughs.get(ReportService.CONSORTIA_DRILLTHROUGH_KEY);
        assertThat(consortiaDrillthrough.get(ReportService.FILTERS_PARAM)).isNotNull();

        Map<String, Object> consortiaDrillthroughFilter = (Map<String, Object>) consortiaDrillthrough.get(ReportService.FILTERS_PARAM);
        assertThat(consortiaDrillthroughFilter.get(ReportService.MEMBER_NAME_FILTER)).isNotNull();
        
        Map<String, Object> memberNameFilter = (Map<String, Object>) consortiaDrillthroughFilter.get(ReportService.MEMBER_NAME_FILTER);
        assertThat(memberNameFilter.get(ReportService.HIDDEN_PARAM)).isNotNull();
        assertThat((boolean) memberNameFilter.get(ReportService.HIDDEN_PARAM)).isTrue();
        
        Mockito.verify(mockApplicationProperties).getHolisticsConsortiaDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsConsortiaDashboardSecret();
        Mockito.verify(mockUserService, Mockito.times(3)).getLoggedInUser();
        Mockito.verify(mockMemberService).getMember(Mockito.eq("salesforce-id"));
        
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testGetConsortiaMemberAffiliationsReportInfo() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("salesforce-id"))).thenReturn(Optional.of(getConsortiumLeadMember()));
        
        ReportInfo reportInfo = reportService.getConsortiaMemberAffiliationsReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo(CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_URL);
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();
        
        Assertions.assertThrows(SignatureException.class, () -> {
            checkCommonClaims(reportInfo.getJwt(), CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_URL); // wrong secret
        });
        
        checkCommonClaims(reportInfo.getJwt(), CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_SECRET);

        Claims claims = parseClaims(reportInfo.getJwt(), CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_SECRET);
        Map<String, Object> drillthroughs = (Map<String, Object>) claims.get(ReportService.DRILLTHROUGHS_PARAM);

        assertThat(drillthroughs).isNotNull();
        assertThat(drillthroughs.get(ReportService.CONSORTIUM_MEMBER_AFFILIATION_REPORT_DRILLTHROUGH_KEY)).isNotNull();
        
        Map<String, Object> drillthrough = (Map<String, Object>) drillthroughs.get(ReportService.CONSORTIUM_MEMBER_AFFILIATION_REPORT_DRILLTHROUGH_KEY);
        assertThat(drillthrough.get(ReportService.FILTER_PARAM)).isNotNull();

        Mockito.verify(mockApplicationProperties).getHolisticsConsortiaMemberAffiliationsDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsConsortiaMemberAffiliationsDashboardSecret();
        Mockito.verify(mockUserService, Mockito.times(2)).getLoggedInUser();
        Mockito.verify(mockMemberService).getMember(Mockito.eq("salesforce-id"));
    }

    @Test
    public void testGetConsortiumReportInfo_IllegalAccess_ConsortiaLeadFalse() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getOtherUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("other-salesforce-id"))).thenReturn(Optional.of(getNonConsortiumLeadMember()));

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            reportService.getConsortiaReportInfo();
        });
    }

    @Test
    public void testGetConsortiumReportInfo_IllegalAccess_ConsortiaLeadNull() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getThirdUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("final-salesforce-id"))).thenReturn(Optional.of(getMemberWithNullConsortiumLead()));

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            reportService.getConsortiaReportInfo();
        });
    }

    private MemberServiceUser getUser() {
        MemberServiceUser user = new MemberServiceUser();
        user.setSalesforceId("salesforce-id");
        return user;
    }
    
    private MemberServiceUser getOtherUser() {
        MemberServiceUser user = new MemberServiceUser();
        user.setSalesforceId("other-salesforce-id");
        return user;
    }

    private MemberServiceUser getThirdUser() {
        MemberServiceUser user = new MemberServiceUser();
        user.setSalesforceId("final-salesforce-id");
        return user;
    }

    private Member getMemberWithNullConsortiumLead() {
        Member member = new Member();
        member.setSalesforceId("final-salesforce-id");
        member.setIsConsortiumLead(null);
        return member;
    }

    private Member getNonConsortiumLeadMember() {
        Member member = new Member();
        member.setSalesforceId("other-salesforce-id");
        member.setIsConsortiumLead(false);
        return member;
    }

    private Member getConsortiumLeadMember() {
        Member member = new Member();
        member.setSalesforceId("salesforce-id");
        member.setIsConsortiumLead(true);
        return member;
    }
    
    private Member getConsortiumMember() {
        Member member = new Member();
        member.setSalesforceId("salesforce-id");
        member.setParentSalesforceId("parent");
        member.setIsConsortiumLead(false);
        return member;
    }
    
    private void checkCommonClaims(String jwt, String secret) {
        Claims claims = parseClaims(jwt, secret);
        assertThat(claims.get(ReportService.SETTINGS_PARAM)).isNotNull();
        assertThat(claims.get(ReportService.PERMISSIONS_PARAM)).isNotNull();
        assertThat(claims.get(ReportService.FILTERS_PARAM)).isNotNull();
        assertThat(claims.get(ReportService.EXP_PARAM)).isNotNull();
    }
    
    private Claims parseClaims(String jwt, String secret) {
        return Jwts.parser().setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).parseClaimsJws(jwt).getBody();
    }

}
