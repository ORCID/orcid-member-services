package org.orcid.domain.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Arrays;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.domain.enumeration.AssertionStatus;

class AssertionUtilsTest {

    private static final String SALESFORCE_ID = "salesforceId";

    private static final String ID_TOKEN = "idToken";

    @Test
    void testGetAffiliationStatusWhereErrorOccured() {
        JSONObject error = getDummyError(404);
        Assertion assertion = getAssertionUpdatedInOrcid();
        assertion.setOrcidError(error.toString());
        assertEquals(AssertionStatus.USER_DELETED_FROM_ORCID.getValue(), AssertionUtils.getAssertionStatus(assertion, getOrcidRecordWithApprovedToken()));

        error = getInvalidScopeError(400);
        assertion = getAssertionUpdatedInOrcid();
        assertion.setOrcidError(error.toString());
        assertEquals(AssertionStatus.USER_REVOKED_ACCESS.getValue(), AssertionUtils.getAssertionStatus(assertion, getOrcidRecordWithRevokedToken()));

        error = getDummyError(500);
        assertion = getAssertionNotAddedToOrcid();
        assertion.setOrcidError(error.toString());
        assertEquals(AssertionStatus.ERROR_ADDING_TO_ORCID.getValue(), AssertionUtils.getAssertionStatus(assertion, getOrcidRecordWithApprovedToken()));

        error = getDummyError(500);
        assertion = getAssertionAddedToOrcid();
        assertion.setOrcidError(error.toString());
        assertEquals(AssertionStatus.ERROR_UPDATING_TO_ORCID.getValue(), AssertionUtils.getAssertionStatus(assertion, getOrcidRecordWithApprovedToken()));

    }

    @Test
    void testGetAffiliationStatusWhereNoErrorOccured() {
        assertEquals(AssertionStatus.USER_DENIED_ACCESS.getValue(), AssertionUtils.getAssertionStatus(getAssertionNotAddedToOrcid(), getOrcidRecordWithDeniedToken()));
        assertEquals(AssertionStatus.PENDING.getValue(), AssertionUtils.getAssertionStatus(getAssertionNotAddedToOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.DELETED_IN_ORCID.getValue(), AssertionUtils.getAssertionStatus(getAssertionDeletedInOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.IN_ORCID.getValue(), AssertionUtils.getAssertionStatus(getAssertionAddedToOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.IN_ORCID.getValue(), AssertionUtils.getAssertionStatus(getAssertionUpdatedInOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.PENDING_RETRY.getValue(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedSinceAddingToOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.PENDING_RETRY.getValue(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedSinceUpdatingInOrcid(), getOrcidRecordWithApprovedToken()));
    }
    
    private Assertion getAssertionNotAddedToOrcid() {
        Assertion assertion = new Assertion();
        assertion.setModified(Instant.now());
        assertion.setSalesforceId(SALESFORCE_ID);
        return assertion;
    }
    
    private Assertion getAssertionAddedToOrcid() {
        Assertion assertion = getAssertionNotAddedToOrcid();
        assertion.setAddedToORCID(Instant.now().plusSeconds(10l));
        assertion.setPutCode("put-code");
        return assertion;
    }
    
    private Assertion getAssertionUpdatedSinceAddingToOrcid() {
        Assertion assertion = getAssertionAddedToOrcid();
        assertion.setModified(Instant.now().plusSeconds(20l));
        return assertion;
    }
    
    private Assertion getAssertionUpdatedInOrcid() {
        Assertion assertion = getAssertionUpdatedSinceAddingToOrcid();
        assertion.setModified(Instant.now().plusSeconds(30l));
        assertion.setUpdatedInORCID(Instant.now().plusSeconds(40l));
        return assertion;
    }
    
    private Assertion getAssertionDeletedInOrcid() {
        Assertion assertion = getAssertionAddedToOrcid();
        assertion.setDeletedFromORCID(Instant.now().plusSeconds(20l));
        return assertion;
    }

    private Assertion getAssertionUpdatedSinceUpdatingInOrcid() {
        Assertion assertion = getAssertionUpdatedInOrcid();
        assertion.setModified(Instant.now().plusSeconds(50l));
        return assertion;
    }

    private OrcidRecord getOrcidRecordWithDeniedToken() {
        OrcidRecord record = new OrcidRecord();
        OrcidToken token = new OrcidToken(SALESFORCE_ID, ID_TOKEN, Instant.now(), null);
        record.setTokens(Arrays.asList(token));
        return record;
    }

    private OrcidRecord getOrcidRecordWithApprovedToken() {
        OrcidRecord record = new OrcidRecord();
        OrcidToken token = new OrcidToken(SALESFORCE_ID, ID_TOKEN, null, null);
        record.setTokens(Arrays.asList(token));
        return record;
    }
    
    private OrcidRecord getOrcidRecordWithRevokedToken() {
        OrcidRecord record = new OrcidRecord();
        OrcidToken token = new OrcidToken(SALESFORCE_ID, ID_TOKEN, null, Instant.now());
        record.setTokens(Arrays.asList(token));
        return record;
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
        error.put("error", "error: invalid_scope");
        return error;
    }

}
