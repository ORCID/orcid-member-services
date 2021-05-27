package org.orcid.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.member.config.ApplicationProperties;
import org.orcid.member.service.reports.ReportInfo;
import org.orcid.member.service.user.MemberServiceUser;

public class ReportServiceTest {

	@Mock
	private ApplicationProperties mockApplicationProperties;
	
	@Mock
	private UserService mockUserService;

	@InjectMocks
	private ReportService reportService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(mockApplicationProperties.getChartioOrgId()).thenReturn("1");
		Mockito.when(mockApplicationProperties.getChartioSecret()).thenReturn("some-secret-long-enough-not-to-case-a-weak-key-exception");
		Mockito.when(mockApplicationProperties.getChartioMemberDashboardId()).thenReturn("2");
		Mockito.when(mockApplicationProperties.getChartioMemberDashboardUrl()).thenReturn("some-dashboard-url");
		Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
	}

	@Test
	public void testGetMemberReportJwt() {
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
	
	private MemberServiceUser getUser() {
		MemberServiceUser user = new MemberServiceUser();
		user.setSalesforceId("salesforce-id");
		return user;
	}

}
