package org.orcid.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.orcid.client.OrcidAPIClient;
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.domain.enumeration.AssertionStatus;
import org.orcid.domain.utils.AssertionUtils;
import org.orcid.repository.AssertionRepository;
import org.orcid.security.SecurityUtils;
import org.orcid.service.assertions.download.impl.AssertionsForEditCsvWriter;
import org.orcid.service.assertions.download.impl.AssertionsReportCsvWriter;
import org.orcid.service.assertions.download.impl.PermissionLinksCsvWriter;
import org.orcid.service.assertions.upload.AssertionsUpload;
import org.orcid.service.assertions.upload.AssertionsUploadSummary;
import org.orcid.service.assertions.upload.impl.AssertionsCsvReader;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.orcid.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Objects;

@Service
public class AssertionService {

    private static final Logger LOG = LoggerFactory.getLogger(AssertionService.class);

    private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private OrcidAPIClient orcidAPIClient;

    @Autowired
    private AssertionsReportCsvWriter assertionsReportCsvWriter;

    @Autowired
    private AssertionsForEditCsvWriter assertionsForEditCsvWriter;

    @Autowired
    private PermissionLinksCsvWriter permissionLinksCsvWriter;

    @Autowired
    private UserService assertionsUserService;

    @Autowired
    private AssertionsCsvReader assertionsCsvReader;

    public boolean assertionExists(String id) {
        return assertionRepository.existsById(id);
    }

    public Page<Assertion> findByOwnerId(Pageable pageable) {
        Page<Assertion> assertionsPage = assertionRepository.findByOwnerId(assertionsUserService.getLoggedInUserId(), pageable);
        assertionsPage.forEach(a -> {
            if (a.getOrcidId() == null) {
                a.setOrcidId(getAssertionOrcidId(a));
                assertionRepository.save(a);
            }

            if (!StringUtils.isBlank(a.getStatus())) {
                a.setStatus(AssertionStatus.valueOf(a.getStatus()).getValue());
            }
        });
        return assertionsPage;
    }

    public List<Assertion> findAllByOwnerId() {
        List<Assertion> assertions = assertionRepository.findAllByOwnerId(assertionsUserService.getLoggedInUserId(), SORT);
        assertions.forEach(a -> {
            // set status as text to display in UI
            if (a.getOrcidId() == null) {
                a.setOrcidId(getAssertionOrcidId(a));
                assertionRepository.save(a);
            }

            if (!StringUtils.isBlank(a.getStatus())) {
                a.setStatus(AssertionStatus.valueOf(a.getStatus()).getValue());
            }
        });
        return assertions;
    }

    public Page<Assertion> findBySalesforceId(Pageable pageable) {
        String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();
        Page<Assertion> assertions = assertionRepository.findBySalesforceId(salesforceId, pageable);
        assertions.forEach(a -> {
            if (a.getOrcidId() == null) {
                a.setOrcidId(getAssertionOrcidId(a));
                assertionRepository.save(a);
            }

            if (!StringUtils.isBlank(a.getStatus())) {
                a.setStatus(AssertionStatus.valueOf(a.getStatus()).getValue());
            }
        });
        return assertions;
    }

    public Page<Assertion> findBySalesforceId(Pageable pageable, String filter) {
        String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();
        Page<Assertion> assertions = assertionRepository
                .findBySalesforceIdAndAffiliationSectionContainingIgnoreCaseOrSalesforceIdAndDepartmentNameContainingIgnoreCaseOrSalesforceIdAndOrgNameContainingIgnoreCaseOrSalesforceIdAndDisambiguatedOrgIdContainingIgnoreCaseOrSalesforceIdAndEmailContainingIgnoreCaseOrSalesforceIdAndOrcidIdContainingIgnoreCaseOrSalesforceIdAndRoleTitleContainingIgnoreCase(
                        pageable, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter,
                        salesforceId, filter);
        assertions.forEach(a -> {
            if (a.getOrcidId() == null) {
                a.setOrcidId(getAssertionOrcidId(a));
                assertionRepository.save(a);
            }

            if (!StringUtils.isBlank(a.getStatus())) {
                LOG.debug("assertion status is: " + a.getStatus());
                a.setStatus(AssertionStatus.valueOf(a.getStatus()).getValue());
            }
        });
        return assertions;
    }

    public void deleteAllBySalesforceId(String salesforceId) {
        List<Assertion> assertions = assertionRepository.findBySalesforceId(salesforceId, SORT);
        assertions.forEach(a -> {
            String assertionEmail = a.getEmail();
            assertionRepository.deleteById(a.getId());

            // Remove OrcidRecord if it has not already been removed
            Optional<OrcidRecord> orcidRecordOptional = orcidRecordService.findOneByEmail(assertionEmail);
            if (orcidRecordOptional.isPresent()) {
                deleteOrcidRecordByEmail(assertionEmail);
            }
        });
        return;
    }

    public Assertion findById(String id) {
        Optional<Assertion> optional = assertionRepository.findById(id);
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Invalid assertion id");
        }
        Assertion assertion = optional.get();
        String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();

        if (!assertion.getSalesforceId().equals(salesforceId)) {
            throw new IllegalArgumentException("Illegal attempt to access assertion of org " + assertion.getSalesforceId());
        }

        if (!StringUtils.isBlank(assertion.getStatus())) {
            LOG.debug("assertion status is: " + assertion.getStatus());
            assertion.setStatus(AssertionStatus.valueOf(assertion.getStatus()).getValue());
        }
        if (assertion.getOrcidId() == null) {
            assertion.setOrcidId(getAssertionOrcidId(assertion));
            assertion.setPermissionLink(orcidRecordService.generateLinkForEmail(assertion.getEmail()));
        }
        return assertion;
    }

    public Assertion createAssertion(Assertion assertion) {
        Instant now = Instant.now();
        AssertionServiceUser user = assertionsUserService.getLoggedInUser();

        assertion.setOwnerId(user.getId());
        assertion.setCreated(now);
        assertion.setModified(now);
        assertion.setLastModifiedBy(user.getEmail());
        assertion.setSalesforceId(assertionsUserService.getLoggedInUserSalesforceId());

        String email = assertion.getEmail();

        Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(email);
        if (!optionalRecord.isPresent()) {
            orcidRecordService.createOrcidRecord(email, now, assertion.getSalesforceId());
        } else {
            OrcidRecord record = optionalRecord.get();
            List<OrcidToken> tokens = record.getTokens();
            boolean createToken = true;
            if (tokens == null || tokens.size() == 0) {
                tokens = new ArrayList<OrcidToken>();
                createToken = true;
            } else {
                for (OrcidToken token : tokens) {
                    if (StringUtils.equals(token.getSalesforceId().trim(), assertion.getSalesforceId().trim())) {
                        createToken = false;
                        break;
                    }
                }
            }

            if (createToken) {
                tokens.add(new OrcidToken(assertion.getSalesforceId(), null, null, null));
                record.setTokens(tokens);
                record.setModified(Instant.now());
                orcidRecordService.updateOrcidRecord(record);
            }

        }
        assertion.setStatus(getAssertionStatus(assertion));
        assertion = assertionRepository.insert(assertion);
        assertion.setStatus(AssertionStatus.valueOf(assertion.getStatus()).getValue());

        if (assertion.getOrcidId() == null) {
            assertion.setOrcidId(getAssertionOrcidId(assertion));
        }

        return assertion;
    }

    public void createAssertions(List<Assertion> assertions) {
        Instant now = Instant.now();
        String ownerId = assertionsUserService.getLoggedInUserId();

        for (Assertion a : assertions) {
            a.setOwnerId(ownerId);
            a.setCreated(now);
            a.setModified(now);
            a.setStatus(getAssertionStatus(a));
            a.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
            assertionRepository.insert(a);
        }
    }

    private void checkAssertionAccess(Assertion existingAssertion) {
        String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();
        if (!salesforceId.equals(existingAssertion.getSalesforceId())) {
            throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation", "affiliationOtherOrganization");
        }
    }

    public Assertion updateAssertion(Assertion assertion) {
        Optional<Assertion> optional = assertionRepository.findById(assertion.getId());
        Assertion existingAssertion = optional.get();
        checkAssertionAccess(existingAssertion);

        AssertionServiceUser user = assertionsUserService.getLoggedInUser();

        copyFieldsToUpdate(assertion, existingAssertion);
        existingAssertion.setModified(Instant.now());
        existingAssertion.setLastModifiedBy(user.getEmail());
        existingAssertion.setStatus(getAssertionStatus(existingAssertion));
        assertion = assertionRepository.save(existingAssertion);
        assertion.setStatus(AssertionStatus.valueOf(assertion.getStatus()).getValue());

        if (assertion.getOrcidId() == null) {
            assertion.setOrcidId(getAssertionOrcidId(assertion));
        }
        return assertion;
    }

    public Assertion updateAssertionSalesforceId(Assertion assertion, String salesForceId) {
        assertion.setSalesforceId(salesForceId);
        assertion.setModified(Instant.now());
        return assertionRepository.save(assertion);
    }

    public void deleteById(String id) {
        Assertion assertion = findById(id);
        String assertionEmail = assertion.getEmail();

        assertionRepository.deleteById(id);

        // Remove OrcidRecord if no other assertions exist for user
        List<Assertion> assertions = assertionRepository.findByEmail(assertionEmail);
        if (assertions.isEmpty()) {
            deleteOrcidRecordByEmail(assertionEmail);
        }
    }

    public List<Assertion> findAssertionsByEmail(String email) {
        return assertionRepository.findByEmail(email);
    }

    public boolean isDuplicate(Assertion assertion) {
        List<Assertion> assertions = assertionRepository.findByEmail(assertion.getEmail());
        for (Assertion a : assertions) {
            if (duplicates(a, assertion)) {
                return true;
            }
        }
        return false;
    }

    private boolean duplicates(Assertion a, Assertion b) {
        if (a.getId() != null && a.getId().equals(b.getId())) {
            return false; // both the same record, not two duplicates
        }

        return !different(a.getAffiliationSection(), b.getAffiliationSection()) && !different(a.getDepartmentName(), b.getDepartmentName())
                && !different(a.getRoleTitle(), b.getRoleTitle()) && !different(a.getStartDay(), b.getStartDay()) && !different(a.getStartMonth(), b.getStartMonth())
                && !different(a.getStartYear(), b.getStartYear()) && !different(a.getEndDay(), b.getEndDay()) && !different(a.getEndMonth(), b.getEndMonth())
                && !different(a.getEndYear(), b.getEndYear()) && !different(a.getOrgName(), b.getOrgName()) && !different(a.getOrgCountry(), b.getOrgCountry())
                && !different(a.getOrgCity(), b.getOrgCity()) && !different(a.getOrgRegion(), b.getOrgRegion())
                && !different(a.getDisambiguationSource(), b.getDisambiguationSource()) && !different(a.getDisambiguatedOrgId(), b.getDisambiguatedOrgId())
                && !different(a.getExternalId(), b.getExternalId()) && !different(a.getExternalIdType(), b.getExternalIdType())
                && !different(a.getExternalIdUrl(), b.getExternalIdUrl()) && !different(a.getUrl(), b.getUrl());
    }

    private boolean different(AffiliationSection affiliationSectionA, AffiliationSection affiliationSectionB) {
        if (affiliationSectionA == null && affiliationSectionB == null) {
            return false;
        }
        return !Objects.equal(affiliationSectionA, affiliationSectionB);
    }

    private boolean different(String fieldA, String fieldB) {
        if ((fieldA == null || fieldA.isEmpty()) && (fieldB == null || fieldB.isEmpty())) {
            return false;
        }
        return !Objects.equal(fieldA, fieldB);
    }

    private boolean assertionToDelete(Assertion assertion) {
        return assertion.getId() != null && assertion.getAddedToORCID() == null && assertion.getAffiliationSection() == null && assertion.getCreated() == null
                && assertion.getDeletedFromORCID() == null && assertion.getDepartmentName() == null && assertion.getDisambiguatedOrgId() == null
                && assertion.getDisambiguationSource() == null && assertion.getEmail() == null && assertion.getEndDay() == null && assertion.getEndMonth() == null
                && assertion.getEndYear() == null && assertion.getExternalId() == null && assertion.getExternalIdType() == null && assertion.getExternalIdUrl() == null
                && assertion.getLastModifiedBy() == null && assertion.getModified() == null && assertion.getOrcidError() == null && assertion.getOrcidId() == null
                && assertion.getOrgCity() == null && assertion.getOrgCity() == null && assertion.getOrgCountry() == null && assertion.getOrgName() == null
                && assertion.getOrgRegion() == null && assertion.getOwnerId() == null && assertion.getPutCode() == null && assertion.getRoleTitle() == null
                && assertion.getSalesforceId() == null && assertion.getStartDay() == null && assertion.getStartMonth() == null && assertion.getStartYear() == null;
    }

    private void copyFieldsToUpdate(Assertion source, Assertion destination) {
        destination.setRoleTitle(source.getRoleTitle());
        destination.setAffiliationSection(source.getAffiliationSection());

        destination.setStartYear(source.getStartYear());
        destination.setStartMonth(source.getStartMonth());
        destination.setStartDay(source.getStartDay());

        destination.setEndYear(source.getEndYear());
        destination.setEndMonth(source.getEndMonth());
        destination.setEndDay(source.getEndDay());

        destination.setExternalId(source.getExternalId());
        destination.setExternalIdType(source.getExternalIdType());
        destination.setExternalIdUrl(source.getExternalIdUrl());

        destination.setOrgCity(source.getOrgCity());
        destination.setOrgCountry(source.getOrgCountry());
        destination.setOrgName(source.getOrgName());
        destination.setOrgRegion(source.getOrgRegion());
        destination.setDisambiguatedOrgId(source.getDisambiguatedOrgId());
        destination.setDisambiguationSource(source.getDisambiguationSource());

        destination.setDepartmentName(source.getDepartmentName());
        destination.setUrl(source.getUrl());
    }

    public void postAssertionsToOrcid() throws JAXBException {
        LOG.info("POSTing affiliations to orcid registry...");
        List<Assertion> assertionsToAdd = assertionRepository.findAllToCreateInOrcidRegistry();
        for (Assertion assertion : assertionsToAdd) {
            LOG.debug("Preparing to POST assertion - id: {}, salesforceId: {}, email: {}, orcid id: {} - to orcid registry");
            postAssertionToOrcid(assertion);
            LOG.debug("POST task complete for assertion {}", assertion.getId());
        }
        LOG.info("POSTing complete");
    }

    public void postAssertionToOrcid(Assertion assertion) throws JAXBException {
        LOG.debug("Examining assertion {} for POSTing to registry", assertion.getId());
        Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(assertion.getEmail());
        if (canSyncWithOrcidRegistry(record, assertion)) {
            String idToken = record.get().getToken(assertion.getSalesforceId());
            String orcid = record.get().getOrcid();

            Instant now = Instant.now();
            assertion.setLastSyncAttempt(now);

            try {
                String putCode = postToOrcidRegistry(orcid, assertion, idToken);
                LOG.debug("Received put code {} from registry", putCode);
                assertion.setPutCode(putCode);
                assertion.setAddedToORCID(now);
                assertion.setUpdatedInORCID(now);
                assertion.setOrcidError(null);
                LOG.debug("Recalculating assertion {} status", assertion.getId());
                assertion.setStatus(AssertionUtils.getAssertionStatus(assertion, record.get()));
                LOG.debug("Updating assertion details in db - lastSyncAttempt: {}, putCode: {}, status: {}, addedToOrcid: {}, error free", now, putCode, assertion.getStatus(), now);
                assertionRepository.save(assertion);
            } catch (ORCIDAPIException oae) {
                LOG.info("Recieved orcid api exception");
                LOG.debug("Orcid api exception details", oae);
                storeError(assertion, oae.getStatusCode(), oae.getError());
            } catch (Exception e) {
                LOG.error("Error posting assertion " + assertion.getId(), e);
                storeError(assertion, 0, e.getMessage());
            }
        }
    }

    public void putAssertionsInOrcid() throws JAXBException {
        LOG.info("PUTting assertions in orcid");
        List<Assertion> assertionsToUpdate = assertionRepository.findAllToUpdateInOrcidRegistry();
        for (Assertion assertion : assertionsToUpdate) {
            // query will return only id and modified dates, so fetch full data
            LOG.debug("Preparing to PUT assertion - id: {}, salesforceId: {}, email: {}, orcid id: {} - in orcid registry");
            Assertion refreshed = assertionRepository.findById(assertion.getId()).get();
            LOG.debug("Refreshed assertion - id: {}, salesforceId: {}, email: {}, orcid id: {}");
            putAssertionInOrcid(refreshed);
            LOG.debug("PUT task complete for assertion {}", assertion.getId());
        }
        LOG.info("PUTting complete");
    }

    public void putAssertionInOrcid(Assertion assertion) throws JAXBException {
        LOG.debug("Examining assertion {} for PUTting in registry", assertion.getId());
        Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(assertion.getEmail());
        if (canSyncWithOrcidRegistry(record, assertion) && !StringUtils.isBlank(assertion.getPutCode())) {
            String orcid = record.get().getOrcid();
            String idToken = record.get().getToken(assertion.getSalesforceId());

            Instant now = Instant.now();
            assertion.setLastSyncAttempt(now);

            try {
                putInOrcidRegistry(orcid, assertion, idToken);
                assertion.setUpdatedInORCID(now);
                assertion.setOrcidError(null);
                LOG.debug("Recalculating assertion {} status", assertion.getId());
                assertion.setStatus(getAssertionStatus(assertion));
                LOG.debug("Updating assertion details in db - lastSyncAttempt: {}, status: {}, updatedInOrcid: {}, error free", now, assertion.getStatus(), now);
                assertionRepository.save(assertion);
            } catch (ORCIDAPIException oae) {
                storeError(assertion, oae.getStatusCode(), oae.getError());
                LOG.info("Recieved orcid api exception");
                LOG.debug("Orcid api exception details", oae);
            } catch (Exception e) {
                LOG.error("Error with assertion " + assertion.getId(), e);
                storeError(assertion, 0, e.getMessage());
            }
        }
    }

    public boolean deleteAssertionFromOrcidRegistry(String assertionId) {
        Assertion assertion = assertionRepository.findById(assertionId).orElseThrow(() -> new IllegalArgumentException("Invalid assertion id"));
        String salesForceId = assertionsUserService.getLoggedInUserSalesforceId();

        if (!salesForceId.equals(assertion.getSalesforceId())) {
            throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation", "affiliationOtherOrganization");
        }

        Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(assertion.getEmail());
        if (canDeleteAssertionFromOrcidRegistry(record, assertion)) {
            Instant now = Instant.now();
            assertion.setLastSyncAttempt(now);

            try {
                LOG.info("Exchanging id token for {}", record.get().getOrcid());
                String accessToken = orcidAPIClient.exchangeToken(record.get().getToken(assertion.getSalesforceId()));
                Boolean deleted = orcidAPIClient.deleteAffiliation(record.get().getOrcid(), accessToken, assertion);
                if (deleted) {
                    assertion.setDeletedFromORCID(now);
                    assertion.setModified(now);
                    assertion.setStatus(getAssertionStatus(assertion));
                    assertionRepository.save(assertion);
                }
                return deleted;
            } catch (ORCIDAPIException oae) {
                storeError(assertion, oae.getStatusCode(), oae.getError());
            } catch (Exception e) {
                LOG.error("Error with assertion " + assertion.getId(), e);
                storeError(assertion, 0, e.getMessage());
            }
        }
        return false;
    }

    public String generateAssertionsReport() throws IOException {
        return assertionsReportCsvWriter.writeCsv();
    }

    private boolean canSyncWithOrcidRegistry(Optional<OrcidRecord> record, Assertion assertion) {
        LOG.debug("Checking if assertion can be synched with registry - id: {}, salesforce id: {}, email: {}, orcid id: {}", assertion.getId(),
                assertion.getSalesforceId(), assertion.getEmail(), assertion.getOrcidId());

        if (!record.isPresent()) {
            LOG.debug("OrcidRecord not available for email {}", assertion.getEmail());
            return false;
        }
        LOG.debug("Orcid record present for email {}", assertion.getEmail());

        String idToken = record.get().getToken(assertion.getSalesforceId());
        String orcid = record.get().getOrcid();

        if (StringUtils.isBlank(orcid)) {
            LOG.debug("No orcid id for assertion {}, can't sync with registry", assertion.getId());
            return false;
        }
        LOG.debug("Found orcid id {}", orcid);
        
        if (StringUtils.isBlank(idToken)) {
            LOG.debug("No id token for assertion {}, can't sync with registry", assertion.getId());
            return false;
        }
        LOG.debug("Found idToken, assertion {} cleared for registry sync", assertion.getId());
        return true;
    }

    private String postToOrcidRegistry(String orcid, Assertion assertion, String idToken) throws JSONException, ClientProtocolException, IOException, JAXBException {
        LOG.info("Exchanging id token for access token for assertion {}, orcid {}", assertion.getId(), orcid);
        String accessToken = orcidAPIClient.exchangeToken(idToken);
        LOG.info("POST affiliation for {} and assertion id {}", orcid, assertion.getId());
        return orcidAPIClient.postAffiliation(orcid, accessToken, assertion);
    }

    private boolean putInOrcidRegistry(String orcid, Assertion assertion, String idToken) throws JSONException, JAXBException, ClientProtocolException, IOException {
        LOG.info("Exchanging id token for access token for assertion {}, orcid {}", assertion.getId(), orcid);
        String accessToken = orcidAPIClient.exchangeToken(idToken);
        LOG.info("PUT affiliation with put-code {} for {} and assertion id {}", assertion.getPutCode(), orcid, assertion.getId());
        return orcidAPIClient.putAffiliation(orcid, accessToken, assertion);
    }

    private boolean canDeleteAssertionFromOrcidRegistry(Optional<OrcidRecord> record, Assertion assertion) {
        if (!record.isPresent()) {
            LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
            return false;
        }
        if (StringUtils.isBlank(record.get().getOrcid())) {
            LOG.info("Orcid ID not available for {}", assertion.getEmail());
            return false;
        }
        if (record.get().getTokens() == null) {
            LOG.info("Tokens not available for {}", assertion.getEmail());
            return false;
        }
        return true;
    }

    private void storeError(Assertion assertion, int statusCode, String error) {
        JSONObject obj = new JSONObject();
        obj.put("statusCode", statusCode);
        obj.put("error", error);
        assertion.setOrcidError(obj.toString());
        assertion.setStatus(getAssertionStatus(assertion));

        if (StringUtils.equals(assertion.getStatus(), AssertionStatus.USER_REVOKED_ACCESS.name())) {
            LOG.debug("Assertion status set to USER_REVOKED_ACCESS, updating id token accordingly");
            orcidRecordService.revokeIdToken(assertion.getEmail(), assertion.getSalesforceId());
        }
        assertionRepository.save(assertion);
    }

    public String getAssertionStatus(Assertion assertion) {
        Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(assertion.getEmail());
        if (!optionalRecord.isPresent()) {
            throw new IllegalArgumentException("Found assertion with no corresponding record email - " + assertion.getEmail() + " - " + assertion.getEmail());
        }
        return AssertionUtils.getAssertionStatus(assertion, optionalRecord.get());
    }

    private String getAssertionOrcidId(Assertion assertion) {
        Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(assertion.getEmail());
        if (optionalRecord.isPresent()) {
            OrcidRecord record = optionalRecord.get();
            if (StringUtils.isBlank(record.getToken(assertion.getSalesforceId()))) {
                return null;
            }
            return record.getOrcid();
        }
        return null;
    }

    public List<Assertion> findByEmail(String email) {
        return assertionRepository.findByEmail(email);
    }

    public List<Assertion> findByEmailAndSalesForceId(String email, String salesForceId) {
        return assertionRepository.findByEmailAndSalesforceId(email, salesForceId);
    }

    private void deleteOrcidRecordByEmail(String email) {
        Optional<OrcidRecord> orcidRecordOptional = orcidRecordService.findOneByEmail(email);
        if (orcidRecordOptional.isPresent()) {
            OrcidRecord orcidRecord = orcidRecordOptional.get();
            orcidRecordService.deleteOrcidRecord(orcidRecord);
        }
    }

    public Optional<Assertion> findOneByEmailIgnoreCase(String email) {
        return assertionRepository.findOneByEmailIgnoreCase(email.toLowerCase());
    }

    public List<Assertion> getAssertionsBySalesforceId(String salesforceId) {
        return assertionRepository.findBySalesforceId(salesforceId);
    }

    public void assertionStatusCleanup() {
        List<Assertion> statusesToClean = assertionRepository.findByStatus("");
        LOG.info("Found " + statusesToClean.size() + " assertion statuses to cleanup.");
        for (Assertion assertion : statusesToClean) {
            assertion.setStatus(getAssertionStatus(assertion));
            assertionRepository.save(assertion);
        }
    }

    public void updateAssertionStatus(AssertionStatus status, Assertion assertion) {
        assertion.setStatus(status.name());
        assertionRepository.save(assertion);
    }

    public String generatePermissionLinks() throws IOException {
        return permissionLinksCsvWriter.writeCsv();
    }

    public String generateAssertionsCSV() throws IOException {
        return assertionsForEditCsvWriter.writeCsv();
    }

    public AssertionsUploadSummary uploadAssertions(MultipartFile file) {
        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        AssertionsUpload upload = readUpload(file, user);
        AssertionsUploadSummary summary = processUpload(upload);
        return summary;
    }

    private AssertionsUploadSummary processUpload(AssertionsUpload upload) {
        AssertionsUploadSummary summary = new AssertionsUploadSummary();

        if (upload.getErrors().size() > 0) {
            summary.setErrors(upload.getErrors());
            return summary;
        }

        int duplicates = 0;
        int created = 0;
        int updated = 0;
        int deleted = 0;

        for (Assertion a : upload.getAssertions()) {
            if (!isDuplicate(a)) {
                if (a.getId() == null || a.getId().isEmpty()) {
                    createAssertion(a);
                    created++;
                } else {
                    Assertion existingAssertion = findById(a.getId());
                    checkAssertionAccess(existingAssertion);

                    if (assertionToDelete(a)) {
                        deleteAssertionFromOrcidRegistry(a.getId());
                        deleteById(a.getId());
                        deleted++;
                    } else {
                        updateAssertion(a);
                        updated++;
                    }
                }
            } else {
                duplicates++;
            }
        }

        summary.setNumAdded(created);
        summary.setNumUpdated(updated);
        summary.setNumDuplicates(duplicates);
        summary.setNumDeleted(deleted);

        return summary;
    }

    private AssertionsUpload readUpload(MultipartFile file, AssertionServiceUser user) {
        InputStream inputStream = null;
        AssertionsUpload upload = null;

        try {
            inputStream = file.getInputStream();
            upload = assertionsCsvReader.readAssertionsUpload(inputStream, user);
        } catch (IOException e) {
            LOG.warn("Error reading user upload", e);
            throw new RuntimeException(e);
        }
        return upload;
    }
}
