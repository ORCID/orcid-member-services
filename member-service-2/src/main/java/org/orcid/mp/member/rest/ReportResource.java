package org.orcid.mp.member.rest;

import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.report.ReportInfo;
import org.orcid.mp.member.service.ReportService;
import org.orcid.mp.member.service.UserService;
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
@RequestMapping("/reports")
public class ReportResource {

    private final Logger LOG = LoggerFactory.getLogger(ReportResource.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @GetMapping("/member")
    public ResponseEntity<ReportInfo> getMemberReport() {
        User user = userService.getLoggedInUser();
        LOG.info("Generating member report for user {} of {}", user.getEmail(), user.getMemberName());
        ReportInfo memberReportInfo = reportService.getMemberReportInfo();
        return ResponseEntity.ok(memberReportInfo);
    }

    @GetMapping("/integration")
    public ResponseEntity<ReportInfo> getIntegrationReport() {
        User user = userService.getLoggedInUser();
        LOG.info("Generating integration report for user {} of {}", user.getEmail(), user.getMemberName());
        ReportInfo memberReportInfo = reportService.getIntegrationReportInfo();
        return ResponseEntity.ok(memberReportInfo);
    }

    @GetMapping("/consortia")
    public ResponseEntity<ReportInfo> getConsortiaReport() {
        User user = userService.getLoggedInUser();
        LOG.info("Generating consortium report for user {} of {}", user.getEmail(), user.getMemberName());
        ReportInfo consortiumReportInfo = reportService.getConsortiaReportInfo();
        return ResponseEntity.ok(consortiumReportInfo);
    }

    @GetMapping("/affiliation")
    public ResponseEntity<ReportInfo> getAffiliationReport() {
        User user = userService.getLoggedInUser();
        LOG.info("Generating affiliation report for user {} of {}", user.getEmail(), user.getMemberName());
        ReportInfo affiliationReportInfo = reportService.getAffiliationReportInfo();
        return ResponseEntity.ok(affiliationReportInfo);
    }

    @GetMapping("/consortia-member-affiliations")
    public ResponseEntity<ReportInfo> getConsortiaMemberAffiliationsReport() {
        User user = userService.getLoggedInUser();
        LOG.info("Generating consortia member affiliations report for user {} of {}", user.getEmail(), user.getMemberName());
        ReportInfo reportInfo = reportService.getConsortiaMemberAffiliationsReportInfo();
        return ResponseEntity.ok(reportInfo);
    }

}