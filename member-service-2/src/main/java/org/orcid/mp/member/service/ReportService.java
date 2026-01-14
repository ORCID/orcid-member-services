package org.orcid.mp.member.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.report.ReportInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class ReportService {

    private final Logger LOG = LoggerFactory.getLogger(ReportService.class);

    static final String FILTERS_PARAM = "filters";

    static final String FILTER_PARAM = "filter";

    static final String DRILLTHROUGHS_PARAM = "drillthroughs";

    static final String SETTINGS_PARAM = "settings";

    static final String PERMISSIONS_PARAM = "permissions";

    static final String EXP_PARAM = "exp";

    static final String PATH_PARAM = "path";

    static final String OPERATOR_PARAM = "operator";

    static final String OPTIONS_PARAM = "options";

    static final String MODIFIER_PARAM = "modifier";

    static final String VALUES_PARAM = "values";

    static final String ROW_BASED_PARAM = "row_based";

    static final String ENABLE_EXPORT_DATA_PARAM = "enable_export_data";

    static final String MEMBER_REPORT_PATH = "client_model.public_sf_members.account_id";

    static final String AFFILIATION_REPORT_PATH = "org_id_centric.public_sf_members.account_id";

    static final String INTEGRATION_REPORT_PATH = "client_model.public_sf_members.account_id";

    static final String CONSORTIA_REPORT_PATH = "client_model.public_sf_consortia.account_id";

    static final String CONSORTIA_FILTER_PATH = "consortia_country_code_model.public_sf_consortia.account_id";

    static final String CONSORTIA_DRILLTHROUGH_KEY = "34687";

    static final String MEMBER_NAME_FILTER = "member_name";

    static final String HIDDEN_PARAM = "hidden";

    static final String DATASET_PARAM = "dataset";

    static final String CONSORTIUM_MEMBER_AFFILIATION_REPORT_DATASET = "org_id_centric";

    static final String MODEL_PARAM = "model";

    static final String CONSORTIUM_MEMBER_AFFILIATION_REPORT_MODEL = "public_sf_consortia";

    static final String FIELD_PARAM = "field";

    static final String CONSORTIUM_MEMBER_AFFILIATION_REPORT_FIELD = "account_id";

    static final String CONSORTIUM_MEMBER_AFFILIATION_REPORT_DRILLTHROUGH_KEY = "38081";

    static final String OPERATOR = "is";

    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Value("${application.reports.holisticsMemberDashboardUrl}")
    private String holisticsMemberDashboardUrl;

    @Value("${application.reports.holisticsMemberDashboardSecret}")
    private String holisticsMemberDashboardSecret;

    @Value("${application.reports.holisticsIntegrationDashboardUrl}")
    private String holisticsIntegrationDashboardUrl;

    @Value("${application.reports.holisticsIntegrationDashboardSecret}")
    private String holisticsIntegrationDashboardSecret;

    @Value("${application.reports.holisticsConsortiaDashboardUrl}")
    private String holisticsConsortiaDashboardUrl;

    @Value("${application.reports.holisticsConsortiaDashboardSecret}")
    private String holisticsConsortiaDashboardSecret;

    @Value("${application.reports.holisticsAffiliationDashboardUrl}")
    private String holisticsAffiliationDashboardUrl;

    @Value("${application.reports.holisticsAffiliationDashboardSecret}")
    private String holisticsAffiliationDashboardSecret;

    @Value("${application.reports.holisticsConsortiaMemberAffiliationsDashboardUrl}")
    private String holisticsConsortiaMemberAffiliationsDashboardUrl;

    @Value("${application.reports.holisticsConsortiaMemberAffiliationsDashboardSecret}")
    private String holisticsConsortiaMemberAffiliationsDashboardSecret;

    public ReportInfo getMemberReportInfo() {
        Map<String, Object> claims = getClaims(getMemberReportPermissions());
        claims.put(FILTERS_PARAM, getMemberNameFilter());

        ReportInfo info = new ReportInfo();
        info.setUrl(holisticsMemberDashboardUrl);
        info.setJwt(getJwt(claims, holisticsMemberDashboardSecret));
        return info;
    }

    public ReportInfo getIntegrationReportInfo() {
        Map<String, Object> claims = getClaims(getIntegrationReportPermissions());


        ReportInfo info = new ReportInfo();
        info.setUrl(holisticsIntegrationDashboardUrl);
        info.setJwt(getJwt(claims,holisticsIntegrationDashboardSecret));
        return info;
    }

    public ReportInfo getConsortiaReportInfo() {
        checkConsortiaLeadAccess();

        Map<String, Object> claims = getClaimsWithDrillthrough(getConsortiaReportPermissions(), CONSORTIA_DRILLTHROUGH_KEY, FILTERS_PARAM, getMemberNameFilter());


        ReportInfo info = new ReportInfo();
        info.setUrl(holisticsConsortiaDashboardUrl);
        info.setJwt(getJwt(claims, holisticsConsortiaDashboardSecret));
        return info;
    }

    public ReportInfo getAffiliationReportInfo() {
        Map<String, Object> claims = getClaims(getAffiliationReportPermissions());
        ReportInfo info = new ReportInfo();
        info.setUrl(holisticsAffiliationDashboardUrl);
        info.setJwt(getJwt(claims, holisticsAffiliationDashboardSecret));
        return info;
    }

    public ReportInfo getConsortiaMemberAffiliationsReportInfo() {
        checkConsortiaLeadAccess();

        Map<String, Object> claims = getClaimsWithDrillthrough(getConsortiaMemberAffiliationsReportPermissions(), CONSORTIUM_MEMBER_AFFILIATION_REPORT_DRILLTHROUGH_KEY,
                FILTER_PARAM, new HashMap<>());
        try {
            LOG.info("report claims are {}", new ObjectMapper().writeValueAsString(claims));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ReportInfo info = new ReportInfo();
        info.setUrl(holisticsConsortiaMemberAffiliationsDashboardUrl);
        info.setJwt(getJwt(claims, holisticsConsortiaMemberAffiliationsDashboardSecret));
        return info;
    }

    private void checkConsortiaLeadAccess() {
        Optional<Member> member = memberService.getMember(getLoggedInSalesforceId());
        if (!Boolean.TRUE.equals(member.get().getIsConsortiumLead())) {
            throw new BadRequestAlertException("Only consortia leads can view consortia reports");
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
        claims.put(EXP_PARAM, exp);
        claims.put(FILTERS_PARAM, new HashMap<String, Object>());

        return claims;
    }

    private Map<String, Object> getClaimsWithDrillthrough(Map<String, Object> permissions, String drillthroughKey, String filterParam, Map<String, Object> filter) {
        Map<String, Object> claims = getClaims(permissions);
        Map<String, Object> drillthroughFilter = new HashMap<>();
        drillthroughFilter.put(filterParam, filter);
        Map<String, Object> drillthroughs = new HashMap<>();
        drillthroughs.put(drillthroughKey, drillthroughFilter);
        claims.put(DRILLTHROUGHS_PARAM, drillthroughs);
        return claims;
    }

    private String getLoggedInSalesforceId() {
        User loggedInUser = userService.getLoggedInUser();
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

    private Map<String, Object> getAffiliationReportPermissions() {
        Map<String, Object> config = getRowBasedConfigBase();
        config.put(PATH_PARAM, AFFILIATION_REPORT_PATH);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(ROW_BASED_PARAM, new Object[] { config });
        return wrapper;
    }

    private Map<String, Object> getConsortiaMemberAffiliationsReportPermissions() {
        Map<String, Object> config = getRowBasedConfigBase();
        config.put(PATH_PARAM, getConsortiaMemberAffiliationsReportPathObject());
        config.put(OPTIONS_PARAM, null);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(ROW_BASED_PARAM, new Object[] { config });
        return wrapper;
    }

    private Map<String, String> getConsortiaMemberAffiliationsReportPathObject() {
        Map<String, String> path = new HashMap<>();
        path.put(DATASET_PARAM, CONSORTIUM_MEMBER_AFFILIATION_REPORT_DATASET);
        path.put(MODEL_PARAM, CONSORTIUM_MEMBER_AFFILIATION_REPORT_MODEL);
        path.put(FIELD_PARAM, CONSORTIUM_MEMBER_AFFILIATION_REPORT_FIELD);
        return path;
    }

    private Map<String, Object> getMemberNameFilter() {
        Map<String, Object> hiddenConfig = new HashMap<>();
        hiddenConfig.put(HIDDEN_PARAM, true);

        Map<String, Object> filters = new HashMap<>();
        filters.put(MEMBER_NAME_FILTER, hiddenConfig);
        return filters;
    }

    private Map<String, Object> getRowBasedConfigBase() {
        Map<String, Object> config = new HashMap<>();
        config.put(OPERATOR_PARAM, OPERATOR);
        config.put(MODIFIER_PARAM, null);
        config.put(VALUES_PARAM, new String[] { getLoggedInSalesforceId() });
        return config;
    }

}
