package org.orcid.mp.member.rest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.report.ReportInfo;
import org.orcid.mp.member.service.ReportService;
import org.orcid.mp.member.service.UserService;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportResourceTest {

    @Mock
    private ReportService mockReportService;

    @Mock
    private UserService mockUserService;

    @InjectMocks
    private ReportResource reportResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mockUserService.getLoggedInUser()).thenReturn(getUser());
    }

    @Test
    public void testGetMemberReport() {
        Mockito.when(mockReportService.getMemberReportInfo()).thenReturn(getReportInfo());
        ResponseEntity<ReportInfo> response = reportResource.getMemberReport();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    public void testGetIntegrationReport() {
        Mockito.when(mockReportService.getIntegrationReportInfo()).thenReturn(getReportInfo());
        ResponseEntity<ReportInfo> response = reportResource.getIntegrationReport();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    public void testGetConsortiumReport() {
        Mockito.when(mockReportService.getConsortiaReportInfo()).thenReturn(getReportInfo());
        ResponseEntity<ReportInfo> response = reportResource.getConsortiaReport();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    public void testGetConsortiaMemberAffiliationsReport() {
        Mockito.when(mockReportService.getConsortiaMemberAffiliationsReportInfo()).thenReturn(getReportInfo());
        ResponseEntity<ReportInfo> response = reportResource.getConsortiaMemberAffiliationsReport();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    public void testGetAffiliationReport() {
        Mockito.when(mockReportService.getAffiliationReportInfo()).thenReturn(getReportInfo());
        ResponseEntity<ReportInfo> response = reportResource.getAffiliationReport();
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    public void testGetConsortiumReportIllegalAccess() {
        Mockito.when(mockReportService.getConsortiaReportInfo()).thenThrow(new BadRequestAlertException("test"));
        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            reportResource.getConsortiaReport();
        });
    }

    private ReportInfo getReportInfo() {
        ReportInfo info = new ReportInfo();
        info.setJwt("jwt");
        info.setUrl("url");
        return info;
    }

    private User getUser() {
        User user = new User();
        user.setEmail("email@orcid.org");
        user.setMemberName("orcid.org");
        return user;
    }
}