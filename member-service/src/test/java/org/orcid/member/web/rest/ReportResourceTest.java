package org.orcid.member.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.member.service.ReportService;
import org.orcid.member.service.reports.ReportInfo;
import org.orcid.member.web.rest.errors.BadRequestAlertException;
import org.springframework.http.ResponseEntity;

public class ReportResourceTest {
	
	@Mock
	private ReportService mockReportService;
	
	@InjectMocks
	private ReportResource reportResource;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testGetMemberReport() {
		Mockito.when(mockReportService.getMemberReportInfo()).thenReturn(getReportInfo());
		ResponseEntity<ReportInfo> response = reportResource.getMemberReport();
		assertThat(response).isNotNull();
		assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_OK);
		assertThat(response.getBody()).isNotNull();
	}
	
	@Test
	public void testGetIntegrationReport() {
		Mockito.when(mockReportService.getIntegrationReportInfo()).thenReturn(getReportInfo());
		ResponseEntity<ReportInfo> response = reportResource.getIntegrationReport();
		assertThat(response).isNotNull();
		assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_OK);
		assertThat(response.getBody()).isNotNull();
	}
	
	@Test
	public void testGetConsortiumReport() {
		Mockito.when(mockReportService.getConsortiumReportInfo()).thenReturn(getReportInfo());
		ResponseEntity<ReportInfo> response = reportResource.getConsortiumReport();
		assertThat(response).isNotNull();
		assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_OK);
		assertThat(response.getBody()).isNotNull();
	}
	
	@Test
	public void testGetConsortiumReportIllegalAccess() {
		Mockito.when(mockReportService.getConsortiumReportInfo()).thenThrow(new BadRequestAlertException("test", null, null));
		Assertions.assertThrows(BadRequestAlertException.class, () -> {
			reportResource.getConsortiumReport();
		});
	}

	private ReportInfo getReportInfo() {
		ReportInfo info = new ReportInfo();
		info.setJwt("jwt");
		info.setUrl("url");
		return info;
	}

}
