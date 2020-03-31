package org.orcid.service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.orcid.client.OrcidAPIClient;
import org.orcid.client.UserSettingsClient;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.repository.AssertionsRepository;
import org.orcid.security.UaaUserUtils;
import org.orcid.service.assertions.report.impl.AssertionsCSVReportWriter;
import org.orcid.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.netflix.hystrix.exception.HystrixRuntimeException;

@Service
public class AssertionsService {
    private final Logger log = LoggerFactory.getLogger(AssertionsService.class);

    private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    @Autowired
    private AssertionsRepository assertionsRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private OrcidAPIClient orcidAPIClient;

    @Autowired
    private UaaUserUtils uaaUserUtils;
    
    @Autowired
    private UserSettingsClient userSettingsClient;
    
    @Autowired
    private AssertionsCSVReportWriter assertionsReportWriter;
    
    public Assertion createOrUpdateAssertion(Assertion assertion) {
    	if (assertion.getId() != null) {
    		return updateAssertion(assertion);
    	} else {
    		return createAssertion(assertion);
    	}
    }
    
    public List<Assertion> createOrUpdateAssertions(List<Assertion> assertions) {
    	assertions.forEach(this::createOrUpdateAssertion);
    	return assertions;
    }
    
    public Page<Assertion> findByOwnerId(Pageable pageable) {
        return assertionsRepository.findByOwnerId(uaaUserUtils.getAuthenticatedUaaUserId(), pageable);
    }

    public List<Assertion> findAllByOwnerId() {
        return assertionsRepository.findAllByOwnerId(uaaUserUtils.getAuthenticatedUaaUserId(), SORT);
    }
    
    public Page<Assertion> findBySalesforceId(Pageable pageable) {
        JSONObject userSettings = getUserSettings(uaaUserUtils.getAuthenticatedUaaUserId());
        String salesforceId = userSettings.getString("salesforceId");
        return assertionsRepository.findBySalesforceId(salesforceId, pageable);
    }

    public Assertion findById(String id) {
        String userUaaId = uaaUserUtils.getAuthenticatedUaaUserId();
        Optional<Assertion> optional = assertionsRepository.findById(id);
        if(!optional.isPresent()) {
            throw new IllegalArgumentException("Invalid assertion id");
        }
        Assertion assertion = optional.get();
        if(!assertion.getOwnerId().equals(userUaaId)) {
            throw new IllegalArgumentException(userUaaId + " is not the owner of " + assertion.getId());
        }
        return assertion;
    }

    public Assertion createAssertion(Assertion assertion) {
        Instant now = Instant.now();
        String ownerId = uaaUserUtils.getAuthenticatedUaaUserId();
        
        assertion.setOwnerId(ownerId);
        assertion.setCreated(now);
        assertion.setModified(now);

        // Store the salesforce id so we can group assertions
        JSONObject userSettings = getUserSettings(ownerId);
        assertion.setSalesforceId(userSettings.getString("salesforceId"));
        
        String email = assertion.getEmail();

        Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(email);
        if (!optionalRecord.isPresent()) {
            orcidRecordService.createOrcidRecord(email, now);
        }

        return assertionsRepository.insert(assertion);
    }

    public void createAssertions(List<Assertion> assertions) {
        Instant now = Instant.now();
        String ownerId = uaaUserUtils.getAuthenticatedUaaUserId();
        // Create assertions
        for (Assertion a : assertions) {
            a.setOwnerId(ownerId);
            a.setCreated(now);
            a.setModified(now);
            // Create the assertion
            assertionsRepository.insert(a);
        }
    }

    public Assertion updateAssertion(Assertion assertion) {
        String uaaUserId = uaaUserUtils.getAuthenticatedUaaUserId();
        Optional<Assertion> optional = assertionsRepository.findById(assertion.getId());
        Assertion existingAssertion = optional.get();

        if (!uaaUserId.equals(existingAssertion.getOwnerId())) {
            throw new IllegalArgumentException("Invalid assertion id");
        }

        copyFieldsToUpdate(assertion, existingAssertion);
        existingAssertion.setUpdated(true);
        existingAssertion.setModified(Instant.now());
        return assertionsRepository.save(existingAssertion);
    }

    public void deleteById(String id) {
        assertionsRepository.deleteById(id);
    }

    private void copyFieldsToUpdate(Assertion source, Assertion destination) {
        // Update start date
        destination.setStartYear(source.getStartYear());
        destination.setStartMonth(source.getStartMonth());
        destination.setStartDay(source.getStartDay());

        // Update end date
        destination.setEndYear(source.getEndYear());
        destination.setEndMonth(source.getEndMonth());
        destination.setEndDay(source.getEndDay());

        // Update external identifiers
        destination.setExternalId(source.getExternalId());
        destination.setExternalIdType(source.getExternalIdType());
        destination.setExternalIdUrl(source.getExternalIdUrl());

        // Update organization
        destination.setOrgCity(source.getOrgCity());
        destination.setOrgCountry(source.getOrgCountry());
        destination.setOrgName(source.getOrgName());
        destination.setOrgRegion(source.getOrgRegion());
        destination.setDisambiguatedOrgId(source.getDisambiguatedOrgId());
        destination.setDisambiguationSource(source.getDisambiguationSource());
        
        // Update department name
        destination.setDepartmentName(source.getDepartmentName());
    }

    public void postAssertionsToOrcid() throws JAXBException {
        List<Assertion> assertionsToAdd = assertionsRepository.findAllToCreate();
        Map<String, String> accessTokens = new HashMap<String, String>();
        for (Assertion assertion : assertionsToAdd) {
            Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(assertion.getEmail());
            if (!optional.isPresent()) {
                log.error("OrcidRecord not available for email {}", assertion.getEmail());
                break;
            }
            OrcidRecord record = optional.get();
            if (StringUtils.isBlank(record.getOrcid())) {
                log.warn("Orcid id still not available for {}", assertion.getEmail());
                break;
            }
            if (StringUtils.isBlank(record.getIdToken())) {
                log.warn("Id token still not available for {}", assertion.getEmail());
                break;
            }

            String orcid = record.getOrcid();
            String idToken = record.getIdToken();
            try {
                String accessToken;
                if (accessTokens.containsKey(orcid)) {
                    accessToken = accessTokens.get(orcid);
                } else {
                    log.info("Exchanging id token for {}", orcid);
                    accessToken = orcidAPIClient.exchangeToken(idToken);
                    accessTokens.put(orcid, idToken);
                }

                log.info("POST affiliation for {} and assertion id {}", orcid, assertion.getId());
                String putCode = orcidAPIClient.postAffiliation(orcid, accessToken, assertion);
                assertion.setPutCode(putCode);
                Instant now = Instant.now();
                assertion.setAddedToORCID(now);
                assertion.setModified(now);
                // Remove error if any
                assertion.setOrcidError(null);
                assertionsRepository.save(assertion);
            } catch (ORCIDAPIException oae) {
                storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
            } catch (Exception e) {
                log.error("Error with assertion " + assertion.getId(), e);
                storeError(assertion.getId(), 0, e.getMessage());
            }
        }
    }

    public void putAssertionsToOrcid() throws JAXBException {
        List<Assertion> assertionsToUpdate = assertionsRepository.findAllToUpdate();
        Map<String, String> accessTokens = new HashMap<String, String>();
        for (Assertion assertion : assertionsToUpdate) {
            Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(assertion.getEmail());
            if (!optional.isPresent()) {
                log.error("OrcidRecord not available for email {}", assertion.getEmail());
                break;
            }
            OrcidRecord record = optional.get();
            if (StringUtils.isBlank(record.getOrcid())) {
                log.warn("Orcid id still not available for {}", assertion.getEmail());
                break;
            }
            if (StringUtils.isBlank(record.getIdToken())) {
                log.warn("Id token still not available for {}", assertion.getEmail());
                break;
            }

            String orcid = record.getOrcid();
            String idToken = record.getIdToken();
            try {
                String accessToken;
                if (accessTokens.containsKey(orcid)) {
                    accessToken = accessTokens.get(orcid);
                } else {
                    log.info("Exchanging id token for {}", orcid);
                    accessToken = orcidAPIClient.exchangeToken(idToken);
                    accessTokens.put(orcid, idToken);
                }
                log.info("PUT affiliation with put-code {} for {} and assertion id {}", assertion.getPutCode(), orcid, assertion.getId());
                orcidAPIClient.putAffiliation(orcid, accessToken, assertion);
                Instant now = Instant.now();
                assertion.setUpdatedInORCID(now);
                assertion.setModified(now);
                // Remove error if any
                assertion.setOrcidError(null);
                assertionsRepository.save(assertion);
            } catch (ORCIDAPIException oae) {
                storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
            } catch (Exception e) {
                log.error("Error with assertion " + assertion.getId(), e);
                storeError(assertion.getId(), 0, e.getMessage());
            }
        }
    }

    public boolean deleteAssertionFromOrcid(String assertionId) throws JSONException, JAXBException {
        Assertion assertion = assertionsRepository.findById(assertionId).orElseThrow(() -> new IllegalArgumentException("Invalid assertion id"));
        String uaaUserId = uaaUserUtils.getAuthenticatedUaaUserId();
        
        if (!uaaUserId.equals(assertion.getOwnerId())) {
            throw new IllegalArgumentException("Invalid assertion id");
        }

        Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(assertion.getEmail());
        if (!optional.isPresent()) {
            log.error("OrcidRecord not available for email {}", assertion.getEmail());
            return false;
        }
        OrcidRecord record = optional.get();
        if (StringUtils.isBlank(record.getOrcid())) {
            log.warn("Orcid id still not available for {}", assertion.getEmail());
            return false;
        }
        if (StringUtils.isBlank(record.getIdToken())) {
            log.warn("Id token still not available for {}", assertion.getEmail());
            return false;
        }

        log.info("Exchanging id token for {}", record.getOrcid());
        try {
            String accessToken = orcidAPIClient.exchangeToken(record.getIdToken());

            Boolean deleted = orcidAPIClient.deleteAffiliation(record.getOrcid(), accessToken, assertion);
            if (deleted) {
                Instant now = Instant.now();
                assertion.setDeletedFromORCID(now);
                assertion.setModified(now);
                assertionsRepository.save(assertion);
            }
            return deleted;
        } catch (ORCIDAPIException oae) {
            storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
        } catch (Exception e) {
            log.error("Error with assertion " + assertion.getId(), e);
            storeError(assertion.getId(), 0, e.getMessage());
        }
        return false;
    }

    public String generateAssertionsReport() throws IOException {
        return assertionsReportWriter.writeAssertionsReport();
    }

    private void storeError(String assertionId, int statusCode, String error) {
        Assertion assertion = assertionsRepository.findById(assertionId).orElseThrow(() -> new RuntimeException("Unable to find assertion with ID: " + assertionId));
        JSONObject obj = new JSONObject();
        obj.put("statusCode", statusCode);
        obj.put("error", error);
        assertion.setOrcidError(obj.toString());
        assertionsRepository.save(assertion);
    }

    private JSONObject getUserSettings(String ownerId) throws JSONException {
        JSONObject existingUserSettings = null;
        try {
            ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(ownerId);
            log.debug("Status code: " + userSettingsResponse.getStatusCodeValue());
            if (userSettingsResponse != null) {
                existingUserSettings = new JSONObject(userSettingsResponse.getBody());
            }
        } catch (HystrixRuntimeException hre) {
            if (hre.getCause() != null && ResponseStatusException.class.isAssignableFrom(hre.getCause().getClass())) {
                ResponseStatusException rse = (ResponseStatusException) hre.getCause();
                if (HttpStatus.NOT_FOUND.equals(rse.getStatus())) {
                    log.debug("User settings not found: " + ownerId);
                } else {
                    throw hre;
                }
            }
        }
        return existingUserSettings;
    }
    
}
