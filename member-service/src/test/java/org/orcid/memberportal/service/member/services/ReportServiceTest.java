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
        Mockito.when(mockApplicationProperties.getHolisticsConsortiaDashboardUrl()).thenReturn("https://secure.holistics.io/embed/consortia");
        Mockito.when(mockApplicationProperties.getHolisticsConsortiaDashboardSecret()).thenReturn("some-long-holistics-consortia-dashboard-secret");
        Mockito.when(mockApplicationProperties.getHolisticsMemberDashboardUrl()).thenReturn("https://secure.holistics.io/embed/member");
        Mockito.when(mockApplicationProperties.getHolisticsMemberDashboardSecret()).thenReturn("some-long-holistics-member-dashboard-secret");
        Mockito.when(mockApplicationProperties.getHolisticsIntegrationDashboardUrl()).thenReturn("https://secure.holistics.io/embed/integration");
        Mockito.when(mockApplicationProperties.getHolisticsIntegrationDashboardSecret()).thenReturn("some-long-holistics-integration-dashboard-secret");
        
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(mockMemberService.getMember(Mockito.eq("salesforce-id"))).thenReturn(Optional.of(getConsortiumLeadMember()));
    }

    @Test
    public void testGetMemberReportInfo() {
        ReportInfo reportInfo = reportService.getMemberReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo("https://secure.holistics.io/embed/member");
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();

        Mockito.verify(mockApplicationProperties).getHolisticsMemberDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsMemberDashboardSecret();
        Mockito.verify(mockUserService).getLoggedInUser();
    }

    @Test
    public void testGetIntegrationReportInfo() {
        ReportInfo reportInfo = reportService.getIntegrationReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo("https://secure.holistics.io/embed/integration");
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();

        Mockito.verify(mockApplicationProperties).getHolisticsIntegrationDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsIntegrationDashboardSecret();
        Mockito.verify(mockUserService).getLoggedInUser();
    }

    @Test
    public void testGetConsortiaReportInfo() {
        ReportInfo reportInfo = reportService.getConsortiaReportInfo();
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getUrl()).isNotNull();
        assertThat(reportInfo.getUrl()).isEqualTo("https://secure.holistics.io/embed/consortia");
        assertThat(reportInfo.getJwt()).isNotNull();
        assertThat(reportInfo.getJwt()).isNotEmpty();

        Mockito.verify(mockApplicationProperties).getHolisticsConsortiaDashboardUrl();
        Mockito.verify(mockApplicationProperties).getHolisticsConsortiaDashboardSecret();
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

}
