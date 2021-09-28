package org.orcid.memberportal.service.assertion.domain.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Arrays;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.OrcidToken;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.domain.utils.AssertionUtils;

class AssertionUtilsTest {

    private static final String SALESFORCE_ID = "salesforceId";

    private static final String ID_TOKEN = "idToken";

    @Test
    void testGetAffiliationStatusWhereErrorOccured() {
        assertEquals(AssertionStatus.USER_DELETED_FROM_ORCID.name(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedInOrcidWithLastSync404Failure(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.USER_REVOKED_ACCESS.name(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedInOrcidWithLastSyncInvalidScopeFailure(), getOrcidRecordWithRevokedToken()));
        assertEquals(AssertionStatus.ERROR_ADDING_TO_ORCID.name(),
                AssertionUtils.getAssertionStatus(getAssertionWhereFirstSyncFailed(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.ERROR_UPDATING_TO_ORCID.name(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedInOrcidWithLastSync500Failure(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.PENDING_RETRY.name(),
                AssertionUtils.getAssertionStatus(getAssertionModifiedSinceLastSyncFailure(), getOrcidRecordWithApprovedToken()));
    }

    @Test
    void testGetAffiliationStatusWhereNoErrorOccured() {
        assertEquals(AssertionStatus.USER_DENIED_ACCESS.name(), AssertionUtils.getAssertionStatus(getAssertionNotAddedToOrcid(), getOrcidRecordWithDeniedToken()));
        assertEquals(AssertionStatus.PENDING.name(), AssertionUtils.getAssertionStatus(getAssertionNotAddedToOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.DELETED_IN_ORCID.name(), AssertionUtils.getAssertionStatus(getAssertionDeletedInOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.IN_ORCID.name(), AssertionUtils.getAssertionStatus(getAssertionAddedToOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.IN_ORCID.name(), AssertionUtils.getAssertionStatus(getAssertionUpdatedInOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.PENDING_RETRY.name(), AssertionUtils.getAssertionStatus(getAssertionUpdatedSinceAddingToOrcid(), getOrcidRecordWithApprovedToken()));
        assertEquals(AssertionStatus.PENDING_RETRY.name(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedSinceUpdatingInOrcid(), getOrcidRecordWithApprovedToken()));
    }

    @Test
    void testGetAffiliationStatusWithTokenIssues() {
        assertEquals(AssertionStatus.USER_DENIED_ACCESS.name(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedSinceUpdatingInOrcid(), getOrcidRecordWithDeniedToken()));
        assertEquals(AssertionStatus.USER_REVOKED_ACCESS.name(),
                AssertionUtils.getAssertionStatus(getAssertionUpdatedSinceUpdatingInOrcid(), getOrcidRecordWithRevokedToken()));
    }

    private Assertion getAssertionModifiedSinceLastSyncFailure() {
        Assertion assertion = getAssertionUpdatedInOrcid();
        assertion.setLastSyncAttempt(Instant.now().plusSeconds(50l));
        assertion.setOrcidError(getDummyError(500).toString());
        assertion.setModified(Instant.now().plusSeconds(55l));
        return assertion;
    }

    private Assertion getAssertionUpdatedInOrcidWithLastSync404Failure() {
        Assertion assertion = getAssertionUpdatedInOrcid();
        assertion.setLastSyncAttempt(Instant.now().plusSeconds(50l));
        assertion.setOrcidError(getDummyError(404).toString());
        return assertion;
    }

    private Assertion getAssertionUpdatedInOrcidWithLastSync500Failure() {
        Assertion assertion = getAssertionUpdatedInOrcid();
        assertion.setLastSyncAttempt(Instant.now().plusSeconds(50l));
        assertion.setOrcidError(getDummyError(600).toString());
        return assertion;
    }

    private Assertion getAssertionUpdatedInOrcidWithLastSyncInvalidScopeFailure() {
        Assertion assertion = getAssertionUpdatedInOrcid();
        assertion.setLastSyncAttempt(Instant.now().plusSeconds(50l));
        assertion.setOrcidError(getInvalidScopeError(400).toString());
        return assertion;
    }

    private Assertion getAssertionNotAddedToOrcid() {
        Assertion assertion = new Assertion();
        assertion.setModified(Instant.now());
        assertion.setSalesforceId(SALESFORCE_ID);
        return assertion;
    }

    private Assertion getAssertionWhereFirstSyncFailed() {
        Assertion assertion = getAssertionNotAddedToOrcid();
        assertion.setLastSyncAttempt(Instant.now());
        assertion.setOrcidError(getDummyError(500).toString());
        return assertion;
    }

    private Assertion getAssertionAddedToOrcid() {
        Assertion assertion = getAssertionNotAddedToOrcid();
        Instant syncDate = Instant.now().plusSeconds(10l);
        assertion.setAddedToORCID(syncDate);
        assertion.setLastSyncAttempt(syncDate);
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
        Instant syncDate = Instant.now().plusSeconds(40l);
        assertion.setUpdatedInORCID(syncDate);
        assertion.setLastSyncAttempt(syncDate);
        return assertion;
    }

    private Assertion getAssertionDeletedInOrcid() {
        Assertion assertion = getAssertionAddedToOrcid();
        Instant syncDate = Instant.now().plusSeconds(20l);
        assertion.setDeletedFromORCID(syncDate);
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
