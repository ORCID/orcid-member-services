package org.orcid.user.domain.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.orcid.user.domain.MemberSettings;

class MemberSettingsValidatorTest {

	@Test
	void testEmptyMember() {
		MemberSettings memberSettings = new MemberSettings();
		assertFalse(MemberSettingsValidator.validate(memberSettings));
	}
	
	@Test
	void testMissingClientId() {
		MemberSettings memberSettings = new MemberSettings();
		memberSettings.setAssertionServiceEnabled(true);
		memberSettings.setIsConsortiumLead(false);
		memberSettings.setSalesforceId("salesforce");
		memberSettings.setParentSalesforceId("parent");
		memberSettings.setClientName("client name");
		assertFalse(MemberSettingsValidator.validate(memberSettings));
	}
	
	@Test
	void testMissingParentSalesforceIdForNonConsortiumLead() {
		MemberSettings memberSettings = new MemberSettings();
		memberSettings.setAssertionServiceEnabled(true);
		memberSettings.setIsConsortiumLead(false);
		memberSettings.setSalesforceId("salesforce");
		memberSettings.setClientId("clientId");
		memberSettings.setClientName("client name");
		assertFalse(MemberSettingsValidator.validate(memberSettings));
	}
	
	@Test
	void testMissingParentSalesforceIdForConsortiumLead() {
		MemberSettings memberSettings = new MemberSettings();
		memberSettings.setAssertionServiceEnabled(true);
		memberSettings.setIsConsortiumLead(true);
		memberSettings.setSalesforceId("salesforce");
		memberSettings.setClientId("clientId");
		memberSettings.setClientName("client name");
		assertTrue(MemberSettingsValidator.validate(memberSettings));
	}

}
