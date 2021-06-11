package org.orcid.member.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.orcid.member.config.ApplicationProperties;
import org.orcid.member.domain.Member;
import org.orcid.member.service.reports.ReportInfo;
import org.orcid.member.service.user.MemberServiceUser;
import org.orcid.member.web.rest.errors.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class ReportService {

	private static final String IAT_PARAM = "iat";

	private static final String EXP_PARAM = "exp";

	private static final String ORG_PARAM = "organization";

	private static final String DASHBOARD_PARAM = "dashboard";

	private static final String ENV_PARAM = "env";

	private static final String SALESFORCE_ID_PARAM = "SF_ID";

	@Autowired
	private UserService userService;
	
	@Autowired
	private MemberService memberService;

	@Autowired
	private ApplicationProperties applicationProperties;

	public ReportInfo getMemberReportInfo() {
		ReportInfo info = new ReportInfo();
		info.setUrl(applicationProperties.getChartioMemberDashboardUrl());
		info.setJwt(getJwt(Integer.valueOf(applicationProperties.getChartioMemberDashboardId())));
		return info;
	}
	
	public ReportInfo getIntegrationReportInfo() {
		ReportInfo info = new ReportInfo();
		info.setUrl(applicationProperties.getChartioIntegrationDashboardUrl());
		info.setJwt(getJwt(Integer.valueOf(applicationProperties.getChartioIntegrationDashboardId())));
		return info;
	}
	
	public ReportInfo getConsortiumReportInfo() {
		checkConsortiumReportAccess();
		ReportInfo info = new ReportInfo();
		info.setUrl(applicationProperties.getChartioConsortiumDashboardUrl());
		info.setJwt(getJwt(Integer.valueOf(applicationProperties.getChartioConsortiumDashboardId())));
		return info;
	}

	private void checkConsortiumReportAccess() {
		Optional<Member> member = memberService.getMember(getLoggedInSalesforceId());
		if (!Boolean.TRUE.equals(member.get().getIsConsortiumLead())) {
			throw new BadRequestAlertException("Only consortia leads can view consortia reports", null, null);
		}
	}

	private String getJwt(int dashboard) {
		long iat = Instant.now().toEpochMilli() / 1000;
		long exp = iat + 900;

		Map<String, Object> envMap = new HashMap<>();
		envMap.put(SALESFORCE_ID_PARAM, getLoggedInSalesforceId());

		Map<String, Object> claims = new HashMap<>();
		claims.put(IAT_PARAM, iat);
		claims.put(EXP_PARAM, exp);
		claims.put(ORG_PARAM, Integer.valueOf(applicationProperties.getChartioOrgId()));
		claims.put(DASHBOARD_PARAM, dashboard);
		claims.put(ENV_PARAM, envMap);

		return Jwts.builder().addClaims(claims)
				.signWith(Keys.hmacShaKeyFor(applicationProperties.getChartioSecret().getBytes(StandardCharsets.UTF_8)))
				.compact();
	}

	private String getLoggedInSalesforceId() {
		MemberServiceUser loggedInUser = userService.getLoggedInUser();
		if (loggedInUser.getLoginAs() != null) {
			loggedInUser = userService.getImpersonatedUser();
		}
		return loggedInUser.getSalesforceId();
	}

}
