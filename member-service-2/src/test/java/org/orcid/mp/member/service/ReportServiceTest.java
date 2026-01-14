package org.orcid.mp.member.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.report.ReportInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
    private UserService mockUserService;

    @Mock
    private MemberService mockMemberService;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(reportService, "holisticsConsortiaDashboardUrl", CONSORTIA_DASHBOARD_URL);
        ReflectionTestUtils.setField(reportService, "holisticsConsortiaDashboardSecret", CONSORTIA_DASHBOARD_SECRET);
        ReflectionTestUtils.setField(reportService, "holisticsMemberDashboardUrl", MEMBER_DASHBOARD_URL);
        ReflectionTestUtils.setField(reportService, "holisticsMemberDashboardSecret", MEMBER_DASHBOARD_SECRET);
        ReflectionTestUtils.setField(reportService, "holisticsAffiliationDashboardUrl", AFFILIATION_DASHBOARD_URL);
        ReflectionTestUtils.setField(reportService, "holisticsAffiliationDashboardSecret", AFFILIATION_DASHBOARD_SECRET);
        ReflectionTestUtils.setField(reportService, "holisticsIntegrationDashboardUrl", INTEGRATION_DASHBOARD_URL);
        ReflectionTestUtils.setField(reportService, "holisticsIntegrationDashboardSecret", INTEGRATION_DASHBOARD_SECRET);
        ReflectionTestUtils.setField(reportService, "holisticsConsortiaMemberAffiliationsDashboardUrl", CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_URL);
        ReflectionTestUtils.setField(reportService, "holisticsConsortiaMemberAffiliationsDashboardSecret", CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_SECRET);
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
            parseClaims(reportInfo.getJwt(), MEMBER_DASHBOARD_URL); // wrong secret
        });

        Claims claims = parseClaims(reportInfo.getJwt(), MEMBER_DASHBOARD_SECRET);
        checkCommonClaims(claims);

        Map<String, Object> filter = (Map<String, Object>) claims.get(ReportService.FILTERS_PARAM);
        assertThat(filter.get(ReportService.MEMBER_NAME_FILTER)).isNotNull();

        Map<String, Object> memberNameFilter = (Map<String, Object>) filter.get(ReportService.MEMBER_NAME_FILTER);
        assertThat(memberNameFilter.get(ReportService.HIDDEN_PARAM)).isNotNull();
        assertThat((boolean) memberNameFilter.get(ReportService.HIDDEN_PARAM)).isTrue();

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
            parseClaims(reportInfo.getJwt(), AFFILIATION_DASHBOARD_URL); // wrong secret
        });

        Claims claims = parseClaims(reportInfo.getJwt(), AFFILIATION_DASHBOARD_SECRET);
        checkCommonClaims(claims);

        Mockito.verify(mockUserService).getLoggedInUser();
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
            parseClaims(reportInfo.getJwt(), INTEGRATION_DASHBOARD_URL); // wrong secret
        });

        Claims claims = parseClaims(reportInfo.getJwt(), INTEGRATION_DASHBOARD_SECRET);
        checkCommonClaims(claims);

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
            parseClaims(reportInfo.getJwt(), CONSORTIA_DASHBOARD_URL); // wrong secret
        });

        Claims claims = parseClaims(reportInfo.getJwt(), CONSORTIA_DASHBOARD_SECRET);
        checkCommonClaims(claims);

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
            parseClaims(reportInfo.getJwt(), CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_URL); // wrong secret
        });

        Claims claims = parseClaims(reportInfo.getJwt(), CONSORTIA_MEMBER_AFFILIATIONS_DASHBOARD_SECRET);
        checkCommonClaims(claims);

        Map<String, Object> drillthroughs = (Map<String, Object>) claims.get(ReportService.DRILLTHROUGHS_PARAM);

        assertThat(drillthroughs).isNotNull();
        assertThat(drillthroughs.get(ReportService.CONSORTIUM_MEMBER_AFFILIATION_REPORT_DRILLTHROUGH_KEY)).isNotNull();

        Map<String, Object> drillthrough = (Map<String, Object>) drillthroughs.get(ReportService.CONSORTIUM_MEMBER_AFFILIATION_REPORT_DRILLTHROUGH_KEY);
        assertThat(drillthrough.get(ReportService.FILTER_PARAM)).isNotNull();

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

    private User getUser() {
        User user = new User();
        user.setSalesforceId("salesforce-id");
        return user;
    }

    private User getOtherUser() {
        User user = new User();
        user.setSalesforceId("other-salesforce-id");
        return user;
    }

    private User getThirdUser() {
        User user = new User();
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

    private void checkCommonClaims(Claims claims) {
        assertThat(claims.get(ReportService.SETTINGS_PARAM)).isNotNull();
        assertThat(claims.get(ReportService.PERMISSIONS_PARAM)).isNotNull();
        assertThat(claims.get(ReportService.EXP_PARAM)).isNotNull();
        assertThat(claims.get(ReportService.FILTERS_PARAM)).isNotNull();
    }

    private Claims parseClaims(String jwt, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)              // Replaces setSigningKey()
                .build()                      // Required: Finalizes the parser configuration
                .parseSignedClaims(jwt)       // Replaces parseClaimsJws()
                .getPayload();
    }

}