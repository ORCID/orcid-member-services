package org.orcid.mp.assertion.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.mp.assertion.client.OrcidApiClient;
import org.orcid.mp.assertion.domain.AssertionStatus;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.error.DeactivatedException;
import org.orcid.mp.assertion.error.DeprecatedException;
import org.orcid.mp.assertion.error.OrcidAPIException;
import org.orcid.mp.assertion.repository.AssertionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.orcid.mp.assertion.domain.Assertion;

@Service
public class AssertionService {

    private static final Logger LOG = LoggerFactory.getLogger(AssertionService.class);

    public static final int REGISTRY_SYNC_BATCH_SIZE = 500;

    private final Sort SORT = Sort.by(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private OrcidApiClient orcidApiClient;

    public void postAssertionsToOrcid() throws JAXBException {
        Pageable pageable = getPageableForRegistrySync();

        LOG.info("POSTing affiliations to orcid registry...");
        List<Assertion> assertionsToAdd = assertionRepository.findAllToCreateInOrcidRegistry(pageable);
        while (assertionsToAdd != null && !assertionsToAdd.isEmpty()) {
            for (Assertion assertion : assertionsToAdd) {
                try {
                    postAssertionToOrcid(assertion);
                } catch (Exception e) {
                    LOG.error("Unexpected error POSTing assertion to registry", e);
                }
            }
            pageable = pageable.next();
            assertionsToAdd = assertionRepository.findAllToCreateInOrcidRegistry(pageable);
        }
        LOG.info("POSTing complete");
    }

    public void postAssertionToOrcid(Assertion assertion) throws JAXBException {
        Optional<OrcidRecord> record = orcidRecordService.findByEmail(assertion.getEmail());
        AssertionStatus deniedStatus = checkForTokenDeniedStatus(record, assertion);

        if (tokenAndOrcidIdAvailable(record, assertion) && deniedStatus == null) {
            OrcidRecord orcidRecord = record.get();
            String idToken = orcidRecord.getToken(assertion.getSalesforceId(), false);
            String orcid = orcidRecord.getOrcid();
            Instant now = Instant.now();
            assertion.setLastSyncAttempt(now);

            try {
                String putCode = postToOrcidRegistry(orcid, assertion, idToken);
                assertion.setPutCode(putCode);
                assertion.setAddedToORCID(now);
                assertion.setOrcidError(null);
                assertion.setStatus(AssertionStatus.IN_ORCID.name());
                assertionRepository.save(assertion);
            } catch (OrcidAPIException oae) {
                LOG.info("Recieved orcid api exception");
                storeError(assertion, oae.getStatusCode(), oae.getError(), AssertionStatus.ERROR_ADDING_TO_ORCID);
            } catch (DeactivatedException | DeprecatedException e) {
                handleDeactivatedOrDeprecated(orcid, assertion);
            } catch (Exception e1) {
                LOG.error("Error posting assertion " + assertion.getId(), e1);
                storeError(assertion, 0, e1.getMessage(), AssertionStatus.ERROR_ADDING_TO_ORCID);
            }
        } else if (deniedStatus != null) {
            assertion.setStatus(deniedStatus.name());
            assertionRepository.save(assertion);
        }
    }

    public void putAssertionsInOrcid() throws JAXBException {
        LOG.info("PUTting assertions in orcid");
        Pageable pageable = getPageableForRegistrySync();
        List<Assertion> assertionsToUpdate = assertionRepository.findAllToUpdateInOrcidRegistry(pageable);
        LOG.info("Fetched {} assertions to update in orcid registry", assertionsToUpdate.size());
        while (assertionsToUpdate != null && !assertionsToUpdate.isEmpty()) {
            for (Assertion assertion : assertionsToUpdate) {
                LOG.info("About to update assertion {} in orcid registry", assertion.getId());
                Assertion refreshed = assertionRepository.findById(assertion.getId()).get();
                try {
                    putAssertionInOrcid(refreshed);
                } catch (Exception e) {
                    LOG.error("Unexpected error PUTting assertion {} in registry", assertion.getId(), e);
                }
            }
            pageable = pageable.next();
            assertionsToUpdate = assertionRepository.findAllToUpdateInOrcidRegistry(pageable);
            LOG.info("Fetched {} assertions to update in orcid registry", assertionsToUpdate.size());
        }
        LOG.info("PUTting complete");
    }

    public void putAssertionInOrcid(Assertion assertion) throws JAXBException {
        Optional<OrcidRecord> record = orcidRecordService.findByEmail(assertion.getEmail());
        AssertionStatus deniedStatus = checkForTokenDeniedStatus(record, assertion);

        if (tokenAndOrcidIdAvailable(record, assertion) && !StringUtils.isBlank(assertion.getPutCode()) && deniedStatus == null) {
            OrcidRecord orcidRecord = record.get();
            String orcid = orcidRecord.getOrcid();
            String idToken = orcidRecord.getToken(assertion.getSalesforceId(), false);
            Instant now = Instant.now();
            assertion.setLastSyncAttempt(now);

            try {
                putInOrcidRegistry(orcid, assertion, idToken);
                assertion.setUpdatedInORCID(now);
                assertion.setOrcidError(null);
                assertion.setStatus(AssertionStatus.IN_ORCID.name());
                assertionRepository.save(assertion);
            } catch (DeactivatedException | DeprecatedException e) {
                handleDeactivatedOrDeprecated(orcid, assertion);
            } catch (OrcidAPIException oae) {
                storeError(assertion, oae.getStatusCode(), oae.getError(), AssertionStatus.ERROR_UPDATING_TO_ORCID);
                LOG.info("Recieved orcid api exception");
            } catch (Exception e) {
                LOG.error("Error with assertion " + assertion.getId(), e);
                storeError(assertion, 0, e.getMessage(), AssertionStatus.ERROR_UPDATING_TO_ORCID);
            }
        } else if (deniedStatus != null) {
            assertion.setStatus(deniedStatus.name());
            assertionRepository.save(assertion);
        }
    }

    private Pageable getPageableForRegistrySync() {
        return PageRequest.of(0, REGISTRY_SYNC_BATCH_SIZE, Sort.by(Sort.Direction.ASC, "created"));
    }

    private AssertionStatus checkForTokenDeniedStatus(Optional<OrcidRecord> orcidRecord, Assertion assertion) {
        if (orcidRecord.isPresent()) {
            if (orcidRecord.get().getRevokedDate(assertion.getSalesforceId()) != null) {
                return AssertionStatus.USER_REVOKED_ACCESS;
            }
            if (orcidRecord.get().getDeniedDate(assertion.getSalesforceId()) != null) {
                return AssertionStatus.USER_DENIED_ACCESS;
            }
        }
        return null;
    }

    private boolean tokenAndOrcidIdAvailable(Optional<OrcidRecord> record, Assertion assertion) {
        if (!record.isPresent()) {
            return false;
        }

        String idToken = record.get().getToken(assertion.getSalesforceId(), false);
        String orcid = record.get().getOrcid();

        if (StringUtils.isBlank(orcid)) {
            return false;
        }

        if (StringUtils.isBlank(idToken)) {
            return false;
        }
        return true;
    }

    private String postToOrcidRegistry(String orcid, Assertion assertion, String idToken) throws IOException, DeprecatedException, DeactivatedException, JSONException {
        LOG.info("Exchanging id token for access token for assertion {}, orcid {}", assertion.getId(), orcid);
        String accessToken = orcidApiClient.exchangeToken(idToken, orcid);
        LOG.info("POST affiliation for {} and assertion id {}", orcid, assertion.getId());
        return orcidApiClient.postAffiliation(orcid, accessToken, assertion);
    }

    private void putInOrcidRegistry(String orcid, Assertion assertion, String idToken) throws JSONException, IOException, DeprecatedException, DeactivatedException {
        LOG.info("Exchanging id token for access token for assertion {}, orcid {}", assertion.getId(), orcid);
        String accessToken = orcidApiClient.exchangeToken(idToken, orcid);
        LOG.info("PUT affiliation with put-code {} for {} and assertion id {}", assertion.getPutCode(), orcid, assertion.getId());
        orcidApiClient.putAffiliation(orcid, accessToken, assertion);
    }

    private void storeError(Assertion assertion, int statusCode, String error, AssertionStatus defaultErrorStatus) {
        LOG.info("Error updating ORCID registry: assertion id - {}, orcid id - {}, status code - {}, error - {}", assertion.getId(), assertion.getOrcidId(), statusCode, error);
        JSONObject obj = new JSONObject();
        try {
            obj.put("statusCode", statusCode);
            obj.put("error", error);
            assertion.setOrcidError(obj.toString());
            assertion.setStatus(getErrorStatus(assertion, defaultErrorStatus).name());
        } catch (JSONException e) {
            LOG.error("Error storing error for assertion {}", assertion.getId(), e);
            throw new RuntimeException(e);
        }

        if (StringUtils.equals(assertion.getStatus(), AssertionStatus.USER_REVOKED_ACCESS.name())) {
            LOG.info("Assertion status set to USER_REVOKED_ACCESS, updating id token accordingly");
            orcidRecordService.revokeIdToken(assertion.getEmail(), assertion.getSalesforceId());
        }
        assertionRepository.save(assertion);
    }

    private AssertionStatus getErrorStatus(Assertion assertion, AssertionStatus defaultError) throws JSONException {
        JSONObject json = new JSONObject(assertion.getOrcidError());
        int statusCode = json.getInt("statusCode");
        String errorMessage = json.getString("error");
        switch (statusCode) {
            case 404:
                return AssertionStatus.USER_DELETED_FROM_ORCID;
            case 401:
                return AssertionStatus.USER_REVOKED_ACCESS;
            case 400:
                if (errorMessage.contains("invalid_scope")) {
                    return AssertionStatus.USER_REVOKED_ACCESS;
                } else {
                    return defaultError;
                }
            default:
                return defaultError;
        }
    }

    private void handleDeactivatedOrDeprecated(String orcid, Assertion assertion) {
        List<Assertion> assertions = assertionRepository.findByEmail(assertion.getEmail());
        assertions.forEach(a -> {
            a.setStatus(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name());
            assertionRepository.save(a);
        });

        orcidRecordService.deleteOrcidRecordByEmail(assertion.getEmail());
    }

}