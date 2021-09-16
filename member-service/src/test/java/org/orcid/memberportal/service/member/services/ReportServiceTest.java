package org.orcid.memberportal.service.member.services;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.orcid.memberportal.service.member.services.MemberService;
import org.orcid.memberportal.service.member.services.ReportService;
import org.orcid.memberportal.service.member.services.UserService;
import org.orcid.memberportal.service.member.web.rest.errors.BadRequestAlertException;

public class ReportServiceTest {

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
        Mockito.when(mockApplicationProperties.getChartioOrgId()).thenReturn("1");
        Mockito.when(mockApplicationProperties.getChartioSecret()).thenReturn("some-secret-long-enough-not-to-case-a-weak-key-exception");
        Mockito.when(mockApplicationProperties.getChartioMemberDashboardId()).thenReturn("2");
        Mockito.when(mockApplicationProperties.getChartioMemberDashboardUrl()).thenReturn("some-dashboard-url");
        Mockito.when(mockApplicationProperties.getChartioIntegrationDashboardId()).thenReturn("3");
        Mockito.when(mockApplicationProperties.getChartioIntegrationDashboardUrl()).thenReturn("some-other-dashboard-url");
        Mockito.when(mockApplicationProperties.getChartioConsortiumDashboardId()).thenReturn("4");
        Mockito.when(mockApplicationProperties.getChartioConsortiumDashboardUrl()).thenReturn("some-final-dashboard-url");
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("salesforce-id"))).thenReturn(Optional.of(getConsortiumLeadMember()));
    }

    @Test
    public void testGetMemberReportInfo() {
        ReportInfo reportInfo = reportService.getMemberReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo("some-dashboard-url");
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();

        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioOrgId();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioSecret();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioMemberDashboardId();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioMemberDashboardUrl();
        Mockito.verify(mockUserService, Mockito.times(1)).getLoggedInUser();
    }

    @Test
    public void testGetIntegrationReportInfo() {
        ReportInfo reportInfo = reportService.getIntegrationReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo("some-other-dashboard-url");
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();

        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioOrgId();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioSecret();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioIntegrationDashboardId();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioIntegrationDashboardUrl();
        Mockito.verify(mockUserService, Mockito.times(1)).getLoggedInUser();
    }

    @Test
    public void testGetConsortiumReportInfo() {
        ReportInfo reportInfo = reportService.getConsortiumReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo("some-final-dashboard-url");
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();

        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioOrgId();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioSecret();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioConsortiumDashboardId();
        Mockito.verify(mockApplicationProperties, Mockito.times(1)).getChartioConsortiumDashboardUrl();
        Mockito.verify(mockUserService, Mockito.times(2)).getLoggedInUser();
        Mockito.verify(mockMemberService, Mockito.times(1)).getMember(Mockito.eq("salesforce-id"));
    }

    @Test
    public void testGetConsortiumReportInfo_IllegalAccess_ConsortiaLeadFalse() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getOtherUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("other-salesforce-id"))).thenReturn(Optional.of(getNonConsortiumLeadMember()));

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            reportService.getConsortiumReportInfo();
        });
    }

    @Test
    public void testGetConsortiumReportInfo_IllegalAccess_ConsortiaLeadNull() {
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getThirdUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("final-salesforce-id"))).thenReturn(Optional.of(getMemberWithNullConsortiumLead()));

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            reportService.getConsortiumReportInfo();
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

}
