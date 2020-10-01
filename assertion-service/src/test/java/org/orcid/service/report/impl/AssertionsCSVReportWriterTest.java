package org.orcid.service.report.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.repository.AssertionsRepository;
import org.orcid.service.UserService;
import org.orcid.service.OrcidRecordService;
import org.orcid.service.assertions.report.impl.AssertionsCSVReportWriter;
import org.springframework.data.domain.Sort;

public class AssertionsCSVReportWriterTest {
	
	private static final String DEFAULT_JHI_USER_ID = "user-id";

	private static final String DEFAULT_LOGIN = "user@orcid.org";

	private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

	@Mock
	private AssertionsRepository assertionsRepository;
	
	@Mock
	private UserService assertionsUserService;
	
	@Mock
	private OrcidRecordService orcidRecordService;
	
	@InjectMocks
	private AssertionsCSVReportWriter reportWriter;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(assertionsRepository.findAllByOwnerId(Mockito.anyString(), Mockito.any(Sort.class))).thenReturn(getListOfAsserions());
		when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
		when(assertionsUserService.getLoggedInUserId()).thenReturn(getUser().getId());
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getDummyOrcidRecord());
	}
	
	private AssertionServiceUser getUser() {
		AssertionServiceUser user = new AssertionServiceUser();
		user.setId(DEFAULT_JHI_USER_ID);
		user.setLogin(DEFAULT_LOGIN);
		user.setSalesforceId(DEFAULT_SALESFORCE_ID);
		return user;
	}
	
	@Test
	public void testWriteAssertionsReport() throws IOException {
		String test = reportWriter.writeAssertionsReport();
		assertNotNull(test);
		assertTrue(test.length() > 0);
	}
	
	private List<Assertion> getListOfAsserions() {
		List<Assertion> assertions = new ArrayList<>();
		for (int i = 0; i < 5; i ++) {
			assertions.add(getDummyAssertion(i));
		}
		return assertions;
	}

	private Assertion getDummyAssertion(int i) {
		Assertion assertion = new Assertion();
        assertion.setAddedToORCID(Instant.now());
        assertion.setModified(Instant.now());
        assertion.setDepartmentName("department-" + i);
        assertion.setAffiliationSection(AffiliationSection.values()[i]);
        assertion.setEmail(i + "@test.com");
        assertion.setEndDay("1");
        assertion.setEndMonth("12");
        assertion.setEndYear("2015");
        assertion.setStartDay("1");
        assertion.setStartMonth("12");
        assertion.setStartYear("2010");
        assertion.setExternalId("123" + i);
        assertion.setExternalIdUrl("http://externalid/" + i);
        assertion.setId(String.valueOf(i));
        assertion.setModified(Instant.now());
        assertion.setOrgCity("city");
        assertion.setOrgCountry("US");
        assertion.setOrgName("org");
        assertion.setOrgRegion("region");
        assertion.setOwnerId("what?" + i);
        assertion.setRoleTitle("role-" + i);
        assertion.setStatus("not sure");
        assertion.setUpdated(true);
        assertion.setUpdatedInORCID(Instant.now());
        return assertion;
	}
	
	private Optional<OrcidRecord> getDummyOrcidRecord() {
		OrcidRecord record = new OrcidRecord();
		record.setCreated(Instant.now());
		record.setEmail("test@test.com");
		record.setId("id");
		List<OrcidToken> tokens = new ArrayList<OrcidToken>();
                OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken");
                tokens.add(newToken);
                record.setTokens(tokens);
		record.setLastNotified(Instant.now());
		record.setModified(Instant.now());
		record.setOrcid("orcid");
		return Optional.of(record);
	}

}
