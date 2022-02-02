package org.orcid.memberportal.service.member.services;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.orcid.memberportal.service.member.config.ApplicationProperties;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.service.reports.ReportInfo;
import org.orcid.memberportal.service.member.service.user.MemberServiceUser;
import org.orcid.memberportal.service.member.web.rest.errors.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class ReportService {

    private static final String FILTERS_PARAM = "filters";

    private static final String SETTINGS_PARAM = "settings";

    private static final String PERMISSIONS_PARAM = "permissions";

    private static final String EXP_PARAM = "exp";

    private static final String PATH_PARAM = "path";

    private static final String OPERATOR_PARAM = "operator";

    private static final String MODIFIER_PARAM = "modifier";

    private static final String VALUES_PARAM = "values";

    private static final String ROW_BASED_PARAM = "row_based";

    private static final String ENABLE_EXPORT_DATA_PARAM = "enable_export_data";

    private static final String MEMBER_REPORT_PATH = "client_model.public_sf_members.account_id";

    private static final String INTEGRATION_REPORT_PATH = "client_model.public_sf_members.account_id";

    private static final String CONSORTIA_REPORT_PATH = "client_model.public_sf_consortia.account_id";
    
    private static final String CONSORTIA_FILTER_PATH = "consortia_country_code_model.public_sf_consortia.account_id";

    private static final String OPERATOR = "is";

    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ApplicationProperties applicationProperties;

    public ReportInfo getMemberReportInfo() {
        ReportInfo info = new ReportInfo();
        info.setUrl(applicationProperties.getHolisticsMemberDashboardUrl());
        info.setJwt(getJwt(getClaims(getMemberReportPermissions()), applicationProperties.getHolisticsMemberDashboardSecret()));
        return info;
    }

    public ReportInfo getIntegrationReportInfo() {
        ReportInfo info = new ReportInfo();
        info.setUrl(applicationProperties.getHolisticsIntegrationDashboardUrl());
        info.setJwt(getJwt(getClaims(getIntegrationReportPermissions()), applicationProperties.getHolisticsIntegrationDashboardSecret()));
        return info;
    }

    public ReportInfo getConsortiaReportInfo() {
        checkConsortiumReportAccess();
        ReportInfo info = new ReportInfo();
        info.setUrl(applicationProperties.getHolisticsConsortiaDashboardUrl());
        info.setJwt(getJwt(getClaims(getConsortiaReportPermissions()), applicationProperties.getHolisticsConsortiaDashboardSecret()));
        return info;
    }

    private void checkConsortiumReportAccess() {
        Optional<Member> member = memberService.getMember(getLoggedInSalesforceId());
        if (!Boolean.TRUE.equals(member.get().getIsConsortiumLead())) {
            throw new BadRequestAlertException("Only consortia leads can view consortia reports", null, null);
        }
    }

    private String getJwt(Map<String, Object> claims, String secret) {
        return Jwts.builder().addClaims(claims).signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
    }

    private Map<String, Object> getClaims(Map<String, Object> permissions) {
        long iat = Instant.now().toEpochMilli() / 1000;
        long exp = iat + 900;

        Map<String, Object> settings = new HashMap<>();
        settings.put(ENABLE_EXPORT_DATA_PARAM, true);

        Map<String, Object> claims = new HashMap<>();
        claims.put(SETTINGS_PARAM, settings);
        claims.put(PERMISSIONS_PARAM, permissions);
        claims.put(FILTERS_PARAM, new HashMap<String, Object>());
        claims.put(EXP_PARAM, exp);

        return claims;
    }

    private String getLoggedInSalesforceId() {
        MemberServiceUser loggedInUser = userService.getLoggedInUser();
        if (loggedInUser.getLoginAs() != null) {
            loggedInUser = userService.getImpersonatedUser();
        }
        return loggedInUser.getSalesforceId();
    }

    private Map<String, Object> getIntegrationReportPermissions() {
        Map<String, Object> config = getRowBasedConfigBase();
        config.put(PATH_PARAM, INTEGRATION_REPORT_PATH);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(ROW_BASED_PARAM, new Object[] { config });
        return wrapper;
    }

    private Map<String, Object> getConsortiaReportPermissions() {
        Map<String, Object> config = getRowBasedConfigBase();
        config.put(PATH_PARAM, CONSORTIA_REPORT_PATH);
        
        Map<String, Object> filter = getRowBasedConfigBase();
        filter.put(PATH_PARAM, CONSORTIA_FILTER_PATH);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(ROW_BASED_PARAM, new Object[] { config, filter });
        return wrapper;
    }

    private Map<String, Object> getMemberReportPermissions() {
        Map<String, Object> config = getRowBasedConfigBase();
        config.put(PATH_PARAM, MEMBER_REPORT_PATH);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(ROW_BASED_PARAM, new Object[] { config });
        return wrapper;
    }

    private Map<String, Object> getRowBasedConfigBase() {
        Map<String, Object> config = new HashMap<>();
        config.put(OPERATOR_PARAM, OPERATOR);
        config.put(MODIFIER_PARAM, null);
        config.put(VALUES_PARAM, new String[] { getLoggedInSalesforceId() });
        return config;
    }

}
