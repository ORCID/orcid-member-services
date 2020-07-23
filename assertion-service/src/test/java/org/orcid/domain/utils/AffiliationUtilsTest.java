package org.orcid.domain.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.enumeration.AssertionStatus;

class AffiliationUtilsTest {

	@Test
	void testGetAffiliationStatusWhereErrorOccured() {
		OrcidRecord record = new OrcidRecord();
		Assertion assertion = new Assertion();

		JSONObject error = getDummyError(404);
		assertion.setOrcidError(error.toString());
		assertEquals(AssertionStatus.USER_DELETED_FROM_ORCID.value,
				AssertionUtils.getAssertionStatus(assertion, record));

		error = getInvalidScopeError(400);
		assertion.setOrcidError(error.toString());
		assertEquals(AssertionStatus.USER_REVOKED_ACCESS.value,
				AssertionUtils.getAssertionStatus(assertion, record));

		error = getDummyError(500);
		assertion.setOrcidError(error.toString());
		assertEquals(AssertionStatus.ERROR_ADDING_TO_ORCID.value,
				AssertionUtils.getAssertionStatus(assertion, record));
	}

	@Test
	void testGetAffiliationStatusWhereNoErrorOccured() {
		OrcidRecord record = new OrcidRecord();
		Assertion assertion = new Assertion();

		record.setDeniedDate(Instant.now());
		assertEquals(AssertionStatus.USER_DENIED_ACCESS.value,
				AssertionUtils.getAssertionStatus(assertion, record));
		record.setDeniedDate(null);

		record.setIdToken("idToken");
		assertEquals(AssertionStatus.USER_GRANTED_ACCESS.value,
				AssertionUtils.getAssertionStatus(assertion, record));
		record.setIdToken(null);

		assertion.setPutCode("put-code");
		assertion.setDeletedFromORCID(Instant.now());
		assertEquals(AssertionStatus.DELETED_IN_ORCID.value,
				AssertionUtils.getAssertionStatus(assertion, record));
		assertion.setDeletedFromORCID(null);

		assertEquals(AssertionStatus.IN_ORCID.value, AssertionUtils.getAssertionStatus(assertion, record));
	}

	private JSONObject getDummyError(int statusCode) {
		JSONObject error = new JSONObject();
		error.put("statusCode", statusCode);
		error.put("error", "dummy");
		return error;
	}
	
	private JSONObject getInvalidScopeError(int statusCode) {
		JSONObject error = new JSONObject();
		error.put("statusCode", statusCode);
		error.put("error", "error: invalid_token");
		return error;
	}

}
