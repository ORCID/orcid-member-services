package org.orcid.memberportal.service.member.web.rest;

import org.orcid.memberportal.service.member.service.reports.ReportInfo;
import org.orcid.memberportal.service.member.services.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for requesting JWTs for chartio reports.
 */
@RestController
@RequestMapping("/api")
public class ReportResource {

    private final Logger LOG = LoggerFactory.getLogger(ReportResource.class);

    @Autowired
    private ReportService reportService;

    @GetMapping("/reports/member")
    public ResponseEntity<ReportInfo> getMemberReport() {
        LOG.debug("Generating member report");
        ReportInfo memberReportInfo = reportService.getMemberReportInfo();
        return ResponseEntity.ok(memberReportInfo);
    }

    @GetMapping("/reports/integration")
    public ResponseEntity<ReportInfo> getIntegrationReport() {
        LOG.debug("Generating integration report");
        ReportInfo memberReportInfo = reportService.getIntegrationReportInfo();
        return ResponseEntity.ok(memberReportInfo);
    }

    @GetMapping("/reports/consortium")
    public ResponseEntity<ReportInfo> getConsortiumReport() {
        LOG.debug("Generating consortium report");
        ReportInfo consortiumReportInfo = reportService.getConsortiumReportInfo();
        return ResponseEntity.ok(consortiumReportInfo);
    }

}
