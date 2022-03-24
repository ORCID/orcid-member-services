package org.orcid.memberportal.service.assertion.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.orcid.memberportal.service.assertion.client.OrcidAPIClient;
import org.orcid.memberportal.service.assertion.csv.CsvWriter;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.CsvReport;
import org.orcid.memberportal.service.assertion.domain.MemberAssertionStatusCount;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.OrcidToken;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.domain.normalization.AssertionNormalizer;
import org.orcid.memberportal.service.assertion.domain.utils.AssertionUtils;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.security.SecurityUtils;
import org.orcid.memberportal.service.assertion.stats.MemberAssertionStats;
import org.orcid.memberportal.service.assertion.upload.AssertionsUpload;
import org.orcid.memberportal.service.assertion.upload.AssertionsUploadSummary;
import org.orcid.memberportal.service.assertion.upload.impl.AssertionsCsvReader;
import org.orcid.memberportal.service.assertion.web.rest.errors.BadRequestAlertException;
import org.orcid.memberportal.service.assertion.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Objects;

@Service
public class AssertionService {

    private static final Logger LOG = LoggerFactory.getLogger(AssertionService.class);
    
    public static final int REGISTRY_SYNC_BATCH_SIZE = 500;

    private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private OrcidAPIClient orcidAPIClient;

    @Autowired
    private UserService assertionsUserService;

    @Autowired
    private AssertionsCsvReader assertionsCsvReader;

    @Autowired
    private AssertionNormalizer assertionNormalizer;

    @Autowired
    private MemberService memberService;

    @Autowired
    private StoredFileService storedFileService;

    @Autowired
    private MailService mailService;

    @Autowired
    private CsvReportService csvReportService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    public boolean assertionExists(String id) {
        return assertionRepository.existsById(id);
    }

    public Page<Assertion> findByOwnerId(Pageable pageable) {
        return assertionRepository.findByOwnerId(assertionsUserService.getLoggedInUserId(), pageable);
    }

    public List<Assertion> findAllByOwnerId() {
        return assertionRepository.findAllByOwnerId(assertionsUserService.getLoggedInUserId(), SORT);
    }

    public Page<Assertion> findBySalesforceId(Pageable pageable) {
        Page<Assertion> assertions = assertionRepository.findBySalesforceId(assertionsUserService.getLoggedInUserSalesforceId(), pageable);
        setPrettyStatus(assertions);
        return assertions;
    }

    public Page<Assertion> findBySalesforceId(Pageable pageable, String filter) {
        String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();
        Page<Assertion> assertions = assertionRepository
                .findBySalesforceIdAndAffiliationSectionContainingIgnoreCaseOrSalesforceIdAndDepartmentNameContainingIgnoreCaseOrSalesforceIdAndOrgNameContainingIgnoreCaseOrSalesforceIdAndDisambiguatedOrgIdContainingIgnoreCaseOrSalesforceIdAndEmailContainingIgnoreCaseOrSalesforceIdAndOrcidIdContainingIgnoreCaseOrSalesforceIdAndRoleTitleContainingIgnoreCase(
                        pageable, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter,
                        salesforceId, filter);
        setPrettyStatus(assertions);
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
        setPrettyStatus(assertion);
        return assertion;
    }

    public void populatePermissionLink(Assertion assertion) {
        assertion.setPermissionLink(orcidRecordService.generateLinkForEmail(assertion.getEmail()));
    }

    public Assertion createAssertion(Assertion assertion, AssertionServiceUser owner) {
        assertion = assertionNormalizer.normalize(assertion);

        Instant now = Instant.now();
        assertion.setOwnerId(owner.getId());
        assertion.setCreated(now);
        assertion.setModified(now);
        assertion.setLastModifiedBy(owner.getEmail());
        assertion.setSalesforceId(owner.getSalesforceId());

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
            } else {
                assertion.setOrcidId(record.getOrcid());
            }

        }
        assertion.setStatus(getAssertionStatus(assertion));
        assertion = assertionRepository.insert(assertion);
        assertion.setStatus(AssertionStatus.valueOf(assertion.getStatus()).getValue());
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

    public Assertion updateAssertion(Assertion assertion, AssertionServiceUser user) {
        assertion = assertionNormalizer.normalize(assertion);

        Optional<Assertion> optional = assertionRepository.findById(assertion.getId());
        Assertion existingAssertion = optional.get();
        if (!user.getSalesforceId().equals(existingAssertion.getSalesforceId())) {
            throw new BadRequestAlertException("Illegal assertion access", "affiliation", "affiliationOtherOrganization");
        }

        copyFieldsToUpdate(assertion, existingAssertion);
        existingAssertion.setModified(Instant.now());
        existingAssertion.setLastModifiedBy(user.getEmail());
        existingAssertion.setStatus(getAssertionStatus(existingAssertion));
        assertion = assertionRepository.save(existingAssertion);
        assertion.setStatus(AssertionStatus.valueOf(assertion.getStatus()).getValue());
        return assertion;
    }

    public Assertion updateAssertionSalesforceId(Assertion assertion, String salesForceId) {
        assertion.setSalesforceId(salesForceId);
        assertion.setModified(Instant.now());
        return assertionRepository.save(assertion);
    }

    public void deleteById(String id, AssertionServiceUser user) {
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
                && assertion.getDepartmentName() == null && assertion.getDisambiguatedOrgId() == null && assertion.getDisambiguationSource() == null
                && assertion.getEmail() == null && assertion.getEndDay() == null && assertion.getEndMonth() == null && assertion.getEndYear() == null
                && assertion.getExternalId() == null && assertion.getExternalIdType() == null && assertion.getExternalIdUrl() == null
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
        Pageable pageable = getPageableForRegistrySync();

        LOG.info("POSTing affiliations to orcid registry...");
        List<Assertion> assertionsToAdd = assertionRepository.findAllToCreateInOrcidRegistry(pageable);
        while (assertionsToAdd != null && !assertionsToAdd.isEmpty()) {
            for (Assertion assertion : assertionsToAdd) {
                LOG.debug("Preparing to POST assertion - id: {}, salesforceId: {}, email: {}, orcid id: {} - to orcid registry", assertion.getId(),
                        assertion.getSalesforceId(), assertion.getEmail(), assertion.getOrcidId());
                try {
                    postAssertionToOrcid(assertion);
                } catch (Exception e) {
                    LOG.error("Unexpected error POSTing assertion to registry", e);
                }
                LOG.debug("POST task complete for assertion {}", assertion.getId());
            }
            pageable = pageable.next();
            assertionsToAdd = assertionRepository.findAllToCreateInOrcidRegistry(pageable);
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
                assertion.setOrcidError(null);
                LOG.debug("Recalculating assertion {} status", assertion.getId());
                assertion.setStatus(AssertionUtils.getAssertionStatus(assertion, record.get()));
                LOG.debug("Updating assertion details in db - lastSyncAttempt: {}, putCode: {}, status: {}, addedToOrcid: {}, error free", now, putCode,
                        assertion.getStatus(), now);
                assertionRepository.save(assertion);
            } catch (ORCIDAPIException oae) {
                LOG.info("Recieved orcid api exception");
                LOG.debug("Orcid api exception details", oae);
                storeError(assertion, oae.getStatusCode(), oae.getError());
            } catch (Exception e) {
                LOG.error("Error posting assertion " + assertion.getId(), e);
                storeError(assertion, 0, e.getMessage());
            }
        } else if (record.isPresent()) {
            updateStatusAndSave(assertion, record.get());
        }
    }

    public void putAssertionsInOrcid() throws JAXBException {
        LOG.info("PUTting assertions in orcid");
        Pageable pageable = getPageableForRegistrySync();
        List<Assertion> assertionsToUpdate = assertionRepository.findAllToUpdateInOrcidRegistry(pageable);
        while (assertionsToUpdate != null && !assertionsToUpdate.isEmpty()) {
            for (Assertion assertion : assertionsToUpdate) {
                // query will return only id and modified dates, so fetch full data
                LOG.debug("Preparing to PUT assertion - id: {}, salesforceId: {}, email: {}, orcid id: {} - in orcid registry", assertion.getId(),
                        assertion.getSalesforceId(), assertion.getEmail(), assertion.getOrcidId());
                Assertion refreshed = assertionRepository.findById(assertion.getId()).get();
                LOG.debug("Refreshed assertion - id: {}, salesforceId: {}, email: {}, orcid id: {}", assertion.getId(), assertion.getSalesforceId(), assertion.getEmail(),
                        assertion.getOrcidId());
                try {
                    putAssertionInOrcid(refreshed);
                } catch (Exception e) {
                    LOG.error("Unexpected error PUTting assertion in registry", e);
                }
                LOG.debug("PUT task complete for assertion {}", assertion.getId());
            }
            pageable = pageable.next();
            assertionsToUpdate = assertionRepository.findAllToUpdateInOrcidRegistry(pageable);
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
        } else if (record.isPresent()) {
            updateStatusAndSave(assertion, record.get());
        }
    }

    public boolean deleteAssertionFromOrcidRegistry(String assertionId, AssertionServiceUser user) {
        Assertion assertion = assertionRepository.findById(assertionId).orElseThrow(() -> new IllegalArgumentException("Invalid assertion id"));
        String salesforceId = user.getSalesforceId();
        checkAssertionAccess(assertion, salesforceId);

        Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(assertion.getEmail());
        if (canDeleteAssertionFromOrcidRegistry(record, assertion)) {
            Instant now = Instant.now();
            assertion.setLastSyncAttempt(now);

            try {
                LOG.info("Exchanging id token for {}", record.get().getOrcid());
                String accessToken = orcidAPIClient.exchangeToken(record.get().getToken(assertion.getSalesforceId()));
                Boolean deleted = orcidAPIClient.deleteAffiliation(record.get().getOrcid(), accessToken, assertion);
                if (deleted) {
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

    private void checkAssertionAccess(Assertion assertion, String salesforceId) {
        if (!salesforceId.equals(assertion.getSalesforceId())) {
            throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation", "affiliationOtherOrganization");
        }
    }

    public void generateAssertionsReport() throws IOException {
        String filename = Instant.now() + "_orcid_report.csv";
        csvReportService.storeCsvReportRequest(assertionsUserService.getLoggedInUserId(), filename, CsvReport.ASSERTIONS_REPORT_TYPE);
    }

    public void generateAndSendMemberAssertionStats() throws IOException {
        List<MemberAssertionStatusCount> counts = assertionRepository.getMemberAssertionStatusCounts();
        Map<String, MemberAssertionStats> stats = getMemberAssertionStats(counts);
        String reportCsv = getMemberAssertionStatsCsv(stats);
        File writtenFile = storedFileService.storeMemberAssertionStatsFile(reportCsv);
        mailService.sendMemberAssertionStatsMail(writtenFile);
    }

    private String getMemberAssertionStatsCsv(Map<String, MemberAssertionStats> stats) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (MemberAssertionStats memberStats : stats.values()) {
            List<String> row = new ArrayList<>();
            row.add(memberStats.getMemberName());
            row.add(Integer.toString(memberStats.getTotalAssertions()));
            row.add(memberStats.getStatusCountsString());
            rows.add(row);
        }

        String[] headers = new String[] { "Member name", "Total affiliations", "Statuses" };
        return new CsvWriter().writeCsv(headers, rows);
    }

    private Map<String, MemberAssertionStats> getMemberAssertionStats(List<MemberAssertionStatusCount> counts) {
        Map<String, MemberAssertionStats> stats = new HashMap<>();
        for (MemberAssertionStatusCount count : counts) {
            if (!stats.containsKey(count.getSalesforceId())) {
                MemberAssertionStats memberStats = new MemberAssertionStats();
                memberStats.setMemberName(memberService.getMemberName(count.getSalesforceId()));
                stats.put(count.getSalesforceId(), memberStats);
            }
            stats.get(count.getSalesforceId()).setStatusCount(count.getStatus(), count.getStatusCount());
        }
        return stats;
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

    private void updateStatusAndSave(Assertion a, OrcidRecord r) {
        a.setStatus(AssertionUtils.getAssertionStatus(a, r));
        assertionRepository.save(a);
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

    public List<Assertion> findByEmail(String email) {
        List<Assertion> assertions = assertionRepository.findByEmail(email);
        setPrettyStatus(assertions);
        return assertions;
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

    public void generatePermissionLinks() {
        String filename = Instant.now() + "_orcid_permission_links.csv";
        csvReportService.storeCsvReportRequest(assertionsUserService.getLoggedInUserId(), filename, CsvReport.PERMISSION_LINKS_TYPE);
    }

    public void generateAssertionsCSV() {
        String filename = Instant.now() + "_affiliations.csv";
        csvReportService.storeCsvReportRequest(assertionsUserService.getLoggedInUserId(), filename, CsvReport.ASSERTIONS_FOR_EDIT_TYPE);
    }

    public void uploadAssertions(MultipartFile file) throws IOException {
        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        storedFileService.storeAssertionsCsvFile(file.getInputStream(), file.getOriginalFilename(), user);
    }

    public void processAssertionUploads() {
        List<StoredFile> pendingUploads = storedFileService.getUnprocessedStoredFilesByType(StoredFileService.ASSERTIONS_CSV_FILE_TYPE);
        pendingUploads.forEach(this::processAssertionsUploadFile);
    }

    private void processAssertionsUploadFile(StoredFile uploadFile) {
        File file = new File(uploadFile.getFileLocation());
        AssertionServiceUser user = assertionsUserService.getUserById(uploadFile.getOwnerId());

        try {
            AssertionsUpload upload = readUpload(file, user);
            AssertionsUploadSummary summary = processUpload(upload, user);
            summary.setFilename(uploadFile.getOriginalFilename());
            summary.setDate(DATE_FORMAT.format(uploadFile.getDateWritten()));
            mailService.sendAssertionsUploadSummaryMail(summary, user);
        } catch (Exception e) {
            if (e.getCause() != null) {
                uploadFile.setError(e.getCause().toString());
            } else {
                uploadFile.setError(e.getClass() + ": " + e.getMessage());
            }
        }

        storedFileService.markAsProcessed(uploadFile);
    }

    private AssertionsUploadSummary processUpload(AssertionsUpload upload, AssertionServiceUser user) {
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
                    createAssertion(a, user);
                    created++;
                } else {
                    Assertion existingAssertion = findById(a.getId());
                    if (!user.getSalesforceId().equals(existingAssertion.getSalesforceId())) {
                        throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation", "affiliationOtherOrganization");
                    }
                    if (assertionToDelete(a)) {
                        deleteAssertionFromOrcidRegistry(a.getId(), user);
                        deleteById(a.getId(), user);
                        deleted++;
                    } else {
                        updateAssertion(a, user);
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

    private AssertionsUpload readUpload(File file, AssertionServiceUser user) {
        InputStream inputStream = null;
        AssertionsUpload upload = null;

        try {
            inputStream = new FileInputStream(file);
            upload = assertionsCsvReader.readAssertionsUpload(inputStream, user);
        } catch (IOException e) {
            LOG.warn("Error reading user upload", e);
            throw new RuntimeException(e);
        }
        return upload;
    }

    public void updateOrcidIdsForEmailAndSalesforceId(String email, String salesforceId) {
        Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(email);
        final String orcid = record.get().getOrcid();
        List<Assertion> assertions = assertionRepository.findAllByEmail(email);
        assertions.stream().filter(a -> a.getOrcidId() == null && salesforceId.equals(a.getSalesforceId())).forEach(a -> {
            a.setOrcidId(orcid);
            assertionRepository.save(a);
        });
    }

    private void setPrettyStatus(Iterable<Assertion> affiliations) {
        affiliations.forEach(this::setPrettyStatus);
    }

    private void setPrettyStatus(Assertion assertion) {
        assertion.setPrettyStatus(AssertionStatus.valueOf(assertion.getStatus()).getValue());
    }

    private Pageable getPageableForRegistrySync() {
        return PageRequest.of(0, REGISTRY_SYNC_BATCH_SIZE, new Sort(Direction.ASC, "created"));
    }
}
