package org.orcid.mp.assertion.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.mp.assertion.client.InternalMemberServiceClient;
import org.orcid.mp.assertion.client.InternalUserServiceClient;
import org.orcid.mp.assertion.client.OrcidApiClient;
import org.orcid.mp.assertion.client.UserServiceClient;
import org.orcid.mp.assertion.csv.CsvWriter;
import org.orcid.mp.assertion.domain.*;
import org.orcid.mp.assertion.error.*;
import org.orcid.mp.assertion.normalizer.AssertionNormalizer;
import org.orcid.mp.assertion.repository.AssertionRepository;
import org.orcid.mp.assertion.security.SecurityUtil;
import org.orcid.mp.assertion.upload.AssertionsCsvReader;
import org.orcid.mp.assertion.upload.AssertionsUpload;
import org.orcid.mp.assertion.upload.AssertionsUploadSummary;
import org.orcid.mp.assertion.util.AssertionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

@Service
public class AssertionService {

    private static final Logger LOG = LoggerFactory.getLogger(AssertionService.class);

    public static final int REGISTRY_SYNC_BATCH_SIZE = 500;

    private final Sort SORT = Sort.by(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private OrcidApiClient orcidApiClient;

    @Autowired
    private StoredFileService storedFileService;

    @Autowired
    private MailService mailService;

    @Autowired
    private InternalMemberServiceClient internalMemberServiceClient;

    @Autowired
    private InternalUserServiceClient internalUserServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private AssertionsCsvReader assertionsCsvReader;

    @Autowired
    private AssertionNormalizer assertionNormalizer;

    @Autowired
    private CsvReportService csvReportService;

    public Assertion findById(String id) {
        Optional<Assertion> optional = assertionRepository.findById(id);
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Invalid assertion id");
        }
        Assertion assertion = optional.get();
        setPrettyStatus(assertion);
        return assertion;
    }

    public boolean assertionExists(String id) {
        return assertionRepository.existsById(id);
    }

    public Assertion createAssertion(Assertion assertion, User owner) {
        assertion = assertionNormalizer.normalize(assertion);

        Instant now = Instant.now();
        assertion.setOwnerId(owner.getId());
        assertion.setCreated(now);
        assertion.setModified(now);
        assertion.setLastModifiedBy(owner.getEmail());
        assertion.setSalesforceId(owner.getSalesforceId());
        assertion.setStatus(getAssertionStatus(assertion));

        String email = assertion.getEmail();

        Optional<OrcidRecord> optionalRecord = orcidRecordService.findByEmail(email);
        if (!optionalRecord.isPresent()) {
            orcidRecordService.createOrcidRecord(email, now, assertion.getSalesforceId());
        } else {
            OrcidRecord record = optionalRecord.get();
            if (record.getTokens() == null || record.getTokens().isEmpty() || !record.tokenExists(assertion.getSalesforceId())) {
                if (record.getTokens() == null) {
                    record.setTokens(new ArrayList<>());
                }
                record.getTokens().add(new OrcidToken(assertion.getSalesforceId(), null));
                record.setModified(Instant.now());
                orcidRecordService.updateOrcidRecord(record);
            } else {
                AssertionStatus tokenDeniedStatus = checkForTokenDeniedStatus(optionalRecord, assertion);
                if (tokenDeniedStatus == null) {
                    String activeToken = record.getToken(assertion.getSalesforceId(), false);
                    if (activeToken != null && !activeToken.isBlank()) {
                        if (StringUtils.isBlank(record.getOrcid())) {
                            LOG.warn("Setting empty orcid id '{}' in affiliation {} for email {} when creating assertion", record.getOrcid(), assertion.getId(), email);
                        }
                        assertion.setOrcidId(record.getOrcid());
                    }
                }
            }
        }

        assertion = assertionRepository.insert(assertion);
        setPrettyStatus(assertion);
        return assertion;
    }

    public Assertion updateAssertion(Assertion assertion, User user) {
        assertion = assertionNormalizer.normalize(assertion);

        Optional<Assertion> optional = assertionRepository.findById(assertion.getId());
        Assertion existingAssertion = optional.get();
        if (!user.getSalesforceId().equals(existingAssertion.getSalesforceId())) {
            throw new BadRequestAlertException("Illegal assertion access");
        }

        copyFieldsToUpdate(assertion, existingAssertion);
        existingAssertion.setModified(Instant.now());
        existingAssertion.setLastModifiedBy(user.getEmail());
        existingAssertion.setStatus(getAssertionStatus(existingAssertion));
        assertion = assertionRepository.save(existingAssertion);
        setPrettyStatus(assertion);
        return assertion;
    }

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

    public void generateAndSendMemberAssertionStats() throws IOException {
        List<MemberAssertionStatusCount> counts = assertionRepository.getMemberAssertionStatusCounts();
        Map<String, MemberAssertionStats> stats = getMemberAssertionStats(counts);
        String reportCsv = getMemberAssertionStatsCsv(stats);
        File writtenFile = storedFileService.storeMemberAssertionStatsFile(reportCsv);
        mailService.sendMemberAssertionStatsMail(writtenFile);
    }

    public void processAssertionUploads() {
        List<StoredFile> pendingUploads = storedFileService.getUnprocessedStoredFilesByType(StoredFileService.ASSERTIONS_CSV_FILE_TYPE);
        pendingUploads.forEach(this::processAssertionsUploadFile);
    }

    public void generateAssertionsReport() throws IOException {
        String filename = Instant.now() + "_orcid_report.csv";
        csvReportService.storeCsvReportRequest(getLoggedInUser().getId(), filename, CsvReport.ASSERTIONS_REPORT_TYPE);
    }

    private String getAssertionStatus(Assertion assertion) {
        Optional<OrcidRecord> optionalRecord = orcidRecordService.findByEmail(assertion.getEmail());
        AssertionStatus tokenDeniedStatus = checkForTokenDeniedStatus(optionalRecord, assertion);
        if (tokenDeniedStatus != null) {
            return tokenDeniedStatus.name();
        } else if (AssertionStatus.ERROR_ADDING_TO_ORCID.name().equals(assertion.getStatus())
                || AssertionStatus.ERROR_UPDATING_TO_ORCID.name().equals(assertion.getStatus())) {
            return AssertionStatus.PENDING_RETRY.name();
        } else if (AssertionStatus.ERROR_DELETING_IN_ORCID.name().equals(assertion.getStatus())) {
            return AssertionStatus.ERROR_DELETING_IN_ORCID.name();
        } else if (AssertionStatus.PENDING.name().equals(assertion.getStatus())) {
            return AssertionStatus.PENDING.name();
        } else if (AssertionStatus.NOTIFICATION_SENT.name().equals(assertion.getStatus())) {
            return AssertionStatus.NOTIFICATION_SENT.name();
        } else if (AssertionStatus.NOTIFICATION_REQUESTED.name().equals(assertion.getStatus())) {
            return AssertionStatus.NOTIFICATION_REQUESTED.name();
        } else if (AssertionStatus.NOTIFICATION_FAILED.name().equals(assertion.getStatus())) {
            return AssertionStatus.NOTIFICATION_FAILED.name();
        } else if (assertion.getAddedToORCID() == null) {
            return AssertionStatus.PENDING.name();
        } else {
            // for IN_ORCID or PENDING_UPDATE switch to PENDING_UPDATE status
            return AssertionStatus.PENDING_UPDATE.name();
        }
    }

    private void processAssertionsUploadFile(StoredFile uploadFile) {
        File file = new File(uploadFile.getFileLocation());
        User user = internalUserServiceClient.getUser(uploadFile.getOwnerId());

        try {
            AssertionsUpload upload = readUpload(file, user);
            AssertionsUploadSummary summary = processUpload(upload, user);
            summary.setFilename(uploadFile.getOriginalFilename());
            summary.setDate(DATE_FORMAT.format(uploadFile.getDateWritten()));
            mailService.sendAssertionsUploadSummaryMail(summary, user);
        } catch (Exception e) {
            LOG.warn("Unexpected error processing assertions CSV upload", e);
            if (e.getCause() != null) {
                uploadFile.setError(e.getCause().toString());
            } else {
                uploadFile.setError(e.getClass() + ": " + e.getMessage());
            }
        }

        storedFileService.markAsProcessed(uploadFile);
    }

    private AssertionsUploadSummary processUpload(AssertionsUpload upload, User user) {
        AssertionsUploadSummary summary = new AssertionsUploadSummary();

        if (upload.getErrors().size() > 0) {
            summary.setErrors(upload.getErrors());
            return summary;
        }

        int duplicates = 0;
        int created = 0;
        int updated = 0;
        int deleted = 0;

        List<String> registryDeleteFailures = new ArrayList<>();

        for (Assertion a : upload.getAssertions()) {
            if (assertionToDelete(a)) {
                try {
                    deleteById(a.getId(), user);
                    deleted++;
                } catch (RegistryDeleteFailureException e) {
                    registryDeleteFailures.add(a.getId());
                }
            } else {
                if (!isDuplicate(a)) {
                    if (a.getId() == null || a.getId().isEmpty()) {
                        createAssertion(a, user);
                        created++;
                    } else {
                        Assertion existingAssertion = findById(a.getId());
                        if (!user.getSalesforceId().equals(existingAssertion.getSalesforceId())) {
                            throw new BadRequestAlertException("This affiliation doesn't belong to your organization");
                        }
                        updateAssertion(a, user);
                        updated++;
                    }
                } else {
                    duplicates++;
                }
            }
        }

        summary.setNumAdded(created);
        summary.setNumUpdated(updated);
        summary.setNumDuplicates(duplicates);
        summary.setNumDeleted(deleted);
        summary.setRegistryDeleteFailures(registryDeleteFailures);

        return summary;
    }

    private AssertionsUpload readUpload(File file, User user) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        return assertionsCsvReader.readAssertionsUpload(inputStream, user);
    }

    public boolean isDuplicate(Assertion assertion) {
        Assertion normalized = assertionNormalizer.normalize(assertion);
        List<Assertion> assertions = assertionRepository.findByEmailAndSalesforceId(assertion.getEmail(), assertion.getSalesforceId());
        for (Assertion a : assertions) {
            if (AssertionUtils.duplicates(normalized, a)) {
                return true;
            }
        }
        return false;
    }

    public void deleteById(String id, User user) throws RegistryDeleteFailureException {
        Assertion assertion = findById(id);
        String salesforceId = user.getSalesforceId();
        checkAssertionAccess(assertion, salesforceId);

        // don't bother trying to delete affiliations for deactivated / deprecated profiles in the registry
        if (!StringUtils.isEmpty(assertion.getPutCode()) && !AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name().equals(assertion.getStatus())) {
            LOG.info("Deleting assertion {} in ORCID registry", id);
            deleteAssertionFromOrcidRegistry(assertion);
        }

        String email = assertion.getEmail();
        assertionRepository.deleteById(id);
        if (assertionRepository.countByEmailAndSalesforceId(email, salesforceId) == 0) {
            orcidRecordService.deleteOrcidRecordTokenByEmailAndSalesforceId(email, salesforceId);
        }
    }

    public Page<Assertion> findByCurrentSalesforceId(Pageable pageable) {
        return findBySalesforceId(getLoggedInUser().getSalesforceId(), pageable);
    }

    public Page<Assertion> findBySalesforceId(Pageable pageable, String filter) {
        String salesforceId = getLoggedInUser().getSalesforceId();
        Page<Assertion> assertions = assertionRepository
                .findBySalesforceIdAndAffiliationSectionContainingIgnoreCaseOrSalesforceIdAndDepartmentNameContainingIgnoreCaseOrSalesforceIdAndOrgNameContainingIgnoreCaseOrSalesforceIdAndDisambiguatedOrgIdContainingIgnoreCaseOrSalesforceIdAndEmailContainingIgnoreCaseOrSalesforceIdAndOrcidIdContainingIgnoreCaseOrSalesforceIdAndRoleTitleContainingIgnoreCase(
                        pageable, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter,
                        salesforceId, filter);
        setPrettyStatus(assertions);
        return assertions;
    }

    public List<Assertion> findByEmail(String email) {
        List<Assertion> assertions = assertionRepository.findByEmail(email);
        setPrettyStatus(assertions);
        return assertions;
    }

    public void populatePermissionLink(Assertion assertion) {
        assertion.setPermissionLink(orcidRecordService.generateLinkForEmail(assertion.getEmail()));
    }

    public void generatePermissionLinks() {
        String filename = Instant.now() + "_orcid_permission_links.csv";
        csvReportService.storeCsvReportRequest(getLoggedInUser().getId(), filename, CsvReport.PERMISSION_LINKS_TYPE);
    }

    public void markPendingAssertionsAsNotificationRequested(String salesforceId) {
        assertionRepository.updateStatusPendingToNotificationRequested(salesforceId);
    }

    public void uploadAssertions(MultipartFile file) throws IOException {
        User user = getLoggedInUser();
        storedFileService.storeAssertionsCsvFile(file.getInputStream(), file.getOriginalFilename(), user);
    }

    public void updateOrcidIdsForEmailAndSalesforceId(String email, String salesforceId) {
        Optional<OrcidRecord> record = orcidRecordService.findByEmail(email);
        if (record.isEmpty()) {
            throw new IllegalArgumentException("Can't find orcid record for email " + email);
        }
        final String orcid = record.get().getOrcid();
        List<Assertion> assertions = assertionRepository.findAllByEmail(email);
        assertions.stream().filter(a -> a.getOrcidId() == null && salesforceId.equals(a.getSalesforceId())).forEach(a -> {
            if (StringUtils.isBlank(orcid)) {
                LOG.warn("Setting empty orcid id '{}' in affiliation {} for email {} after granting permission", orcid, a.getId(), email);
            }
            a.setOrcidId(orcid);
            assertionRepository.save(a);
        });
    }

    public void generateAssertionsCSV() {
        String filename = Instant.now() + "_affiliations.csv";
        csvReportService.storeCsvReportRequest(getLoggedInUser().getId(), filename, CsvReport.ASSERTIONS_FOR_EDIT_TYPE);
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

        return !StringUtils.isBlank(idToken);
    }

    private void checkAssertionAccess(Assertion assertion, String salesforceId) {
        if (!salesforceId.equals(assertion.getSalesforceId())) {
            throw new BadRequestAlertException("This affiliations doesnt belong to your organization");
        }
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

    private String getMemberAssertionStatsCsv(Map<String, MemberAssertionStats> stats) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (MemberAssertionStats memberStats : stats.values()) {
            List<String> row = new ArrayList<>();
            row.add(memberStats.getMemberName());
            row.add(Integer.toString(memberStats.getTotalAssertions()));
            row.add(memberStats.getStatusCountsString());
            rows.add(row);
        }

        String[] headers = new String[]{"Member name", "Total affiliations", "Statuses"};
        return new CsvWriter().writeCsv(headers, rows);
    }

    private Map<String, MemberAssertionStats> getMemberAssertionStats(List<MemberAssertionStatusCount> counts) {
        Map<String, MemberAssertionStats> stats = new HashMap<>();
        for (MemberAssertionStatusCount count : counts) {
            if (!stats.containsKey(count.getSalesforceId())) {
                MemberAssertionStats memberStats = new MemberAssertionStats();
                memberStats.setMemberName(getMemberNameWithInternalScope(count.getSalesforceId()));
                stats.put(count.getSalesforceId(), memberStats);
            }
            stats.get(count.getSalesforceId()).setStatusCount(count.getStatus(), count.getStatusCount());
        }
        return stats;
    }

    private String getMemberNameWithInternalScope(String salesforceId) {
        Member member = internalMemberServiceClient.getMember(salesforceId);
        if (member != null) {
            return member.getClientName();
        }
        throw new RuntimeException("Member with salesforce id " + salesforceId + " not found");
    }

    private void setPrettyStatus(Assertion assertion) {
        if (assertion.getStatus() != null) {
            assertion.setPrettyStatus(AssertionStatus.valueOf(assertion.getStatus()).getValue());
        }
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

    private void deleteAssertionFromOrcidRegistry(Assertion assertion) throws RegistryDeleteFailureException {
        Optional<OrcidRecord> record = orcidRecordService.findByEmail(assertion.getEmail());
        String orcidId = record.get().getOrcid();

        if (!checkRegistryDeletePreconditions(record, assertion)) {
            throw new RegistryDeleteFailureException();
        }

        assertion.setLastSyncAttempt(Instant.now());

        try {
            LOG.info("Exchanging id token for {}", orcidId);
            String accessToken = orcidApiClient.exchangeToken(record.get().getToken(assertion.getSalesforceId(), true), orcidId);
            orcidApiClient.deleteAffiliation(orcidId, accessToken, assertion);
        } catch (DeactivatedException | DeprecatedException e) {
            handleDeactivatedOrDeprecated(orcidId, assertion);
        } catch (OrcidAPIException oae) {
            if (oae.getStatusCode() != 404 && !AssertionStatus.USER_REVOKED_ACCESS.name().equals(assertion.getStatus())) {
                storeError(assertion, oae.getStatusCode(), oae.getError(), AssertionStatus.ERROR_DELETING_IN_ORCID);
                throw new RegistryDeleteFailureException();
            }
        } catch (Exception e) {
            if (!AssertionStatus.USER_REVOKED_ACCESS.name().equals(assertion.getStatus())) {
                storeError(assertion, 0, e.getMessage(), AssertionStatus.ERROR_DELETING_IN_ORCID);
                throw new RegistryDeleteFailureException();
            }
        }
    }

    private boolean checkRegistryDeletePreconditions(Optional<OrcidRecord> record, Assertion assertion) {
        String error = null;
        if (!record.isPresent()) {
            LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
            error = "Orcid record not available";
        } else if (StringUtils.isBlank(record.get().getOrcid())) {
            LOG.info("Orcid ID not available for {}", assertion.getEmail());
            error = "ORCID iD not available";
        } else if (record.get().getTokens() == null || record.get().getToken(assertion.getSalesforceId(), true) == null) {
            LOG.info("Token not available for {}", assertion.getEmail());
            error = "Token not available";
        }
        if (error != null) {
            assertion.setOrcidError(error);
            assertion.setStatus(AssertionStatus.ERROR_DELETING_IN_ORCID.name());
            assertionRepository.save(assertion);
            return false;
        }
        return true;
    }

    private User getLoggedInUser() {
        String userLogin = SecurityUtil.getCurrentUserLogin().get();
        return userServiceClient.getUser(userLogin);
    }

    private Page<Assertion> findBySalesforceId(String salesforceId, Pageable pageable) {
        Page<Assertion> assertions = assertionRepository.findBySalesforceId(salesforceId, pageable);
        setPrettyStatus(assertions);
        return assertions;
    }

    private void setPrettyStatus(Iterable<Assertion> affiliations) {
        affiliations.forEach(this::setPrettyStatus);
    }
}