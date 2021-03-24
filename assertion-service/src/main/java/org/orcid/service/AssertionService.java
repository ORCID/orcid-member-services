package org.orcid.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.orcid.client.OrcidAPIClient;
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.domain.enumeration.AssertionStatus;
import org.orcid.domain.utils.AssertionUtils;
import org.orcid.repository.AssertionsRepository;
import org.orcid.security.SecurityUtils;
import org.orcid.service.assertions.download.impl.AssertionsForEditCsvWriter;
import org.orcid.service.assertions.download.impl.AssertionsReportCsvWriter;
import org.orcid.service.assertions.download.impl.PermissionLinksCsvWriter;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.orcid.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AssertionService {

	private static final Logger LOG = LoggerFactory.getLogger(AssertionService.class);

	private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified",
			"deletedFromORCID");

	@Autowired
	private AssertionsRepository assertionsRepository;

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

	public boolean assertionExists(String id) {
		return assertionsRepository.existsById(id);
	}

	public Page<Assertion> findByOwnerId(Pageable pageable) {
		Page<Assertion> assertionsPage = assertionsRepository.findByOwnerId(assertionsUserService.getLoggedInUserId(),
				pageable);
		assertionsPage.forEach(a -> {
			// set status as text to display in UI
			if (!StringUtils.isBlank(a.getStatus())) {
				a.setStatus(AssertionStatus.getStatus(a.getStatus()).getText());
			}
			if (a.getOrcidId() == null) {
				a.setOrcidId(getAssertionOrcidId(a));
			}
		});
		return assertionsPage;
	}

	public List<Assertion> findAllByOwnerId() {
		List<Assertion> assertions = assertionsRepository.findAllByOwnerId(assertionsUserService.getLoggedInUserId(),
				SORT);
		assertions.forEach(a -> {
			// set status as text to display in UI
			if (!StringUtils.isBlank(a.getStatus())) {
				a.setStatus(AssertionStatus.getStatus(a.getStatus()).getText());
			}

			if (a.getOrcidId() == null) {
				a.setOrcidId(getAssertionOrcidId(a));
			}
		});
		return assertions;
	}

	public Page<Assertion> findBySalesforceId(Pageable pageable) {
		String salesForceId = assertionsUserService.getLoggedInUserSalesforceId();
		Page<Assertion> assertions = assertionsRepository.findBySalesforceId(salesForceId, pageable);
		assertions.forEach(a -> {
			if (!StringUtils.isBlank(a.getStatus())) {
				LOG.debug("assertion status is: " + a.getStatus());
				a.setStatus(AssertionStatus.getStatus(a.getStatus()).getText());
			}

			if (a.getOrcidId() == null) {
				a.setOrcidId(getAssertionOrcidId(a));
			}
		});
		return assertions;
	}

	public void deleteAllBySalesforceId(String salesforceId) {
		List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesforceId, SORT);
		assertions.forEach(a -> {
			String assertionEmail = a.getEmail();
			assertionsRepository.deleteById(a.getId());

			// Remove OrcidRecord if it has not already been removed
			Optional<OrcidRecord> orcidRecordOptional = orcidRecordService.findOneByEmail(assertionEmail);
			if (orcidRecordOptional.isPresent()) {
				deleteOrcidRecordByEmail(assertionEmail);
			}
		});
		return;
	}

	public Assertion findById(String id) {
		Optional<Assertion> optional = assertionsRepository.findById(id);
		if (!optional.isPresent()) {
			throw new IllegalArgumentException("Invalid assertion id");
		}
		Assertion assertion = optional.get();
		String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();

		if (!assertion.getSalesforceId().equals(salesforceId)) {
			throw new IllegalArgumentException(
					"Illegal attempt to access assertion of org " + assertion.getSalesforceId());
		}

		if (!StringUtils.isBlank(assertion.getStatus())) {
			LOG.debug("assertion status is: " + assertion.getStatus());
			assertion.setStatus(AssertionStatus.getStatus(assertion.getStatus()).getText());
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
		assertion.setLastModifiedBy(user.getLogin());
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
					if (StringUtils.equals(token.getSalesforce_id().trim(), assertion.getSalesforceId().trim())) {
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
		assertion = assertionsRepository.insert(assertion);
		assertion.setStatus(AssertionStatus.getStatus(assertion.getStatus()).getText());

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
			assertionsRepository.insert(a);
		}
	}

	private void checkAssertionAccess(Assertion existingAssertion) {
		String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();
		if (!salesforceId.equals(existingAssertion.getSalesforceId())) {
			throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation",
					"affiliationOtherOrganization");
		}
	}

	public Assertion updateAssertion(Assertion assertion) {
		Optional<Assertion> optional = assertionsRepository.findById(assertion.getId());
		Assertion existingAssertion = optional.get();
		checkAssertionAccess(existingAssertion);

		AssertionServiceUser user = assertionsUserService.getLoggedInUser();

		copyFieldsToUpdate(assertion, existingAssertion);
		existingAssertion.setUpdated(true);
		existingAssertion.setModified(Instant.now());
		existingAssertion.setLastModifiedBy(user.getLogin());
		existingAssertion.setStatus(getAssertionStatus(existingAssertion));
		assertion = assertionsRepository.save(existingAssertion);
		assertion.setStatus(AssertionStatus.getStatus(assertion.getStatus()).getText());

		if (assertion.getOrcidId() == null) {
			assertion.setOrcidId(getAssertionOrcidId(assertion));
		}
		return assertion;
	}

	public Assertion updateAssertionSalesforceId(Assertion assertion, String salesForceId) {
		assertion.setSalesforceId(salesForceId);
		assertion.setUpdated(true);
		assertion.setModified(Instant.now());
		return assertionsRepository.save(assertion);
	}

	public void createUpdateOrDeleteAssertion(Assertion a) {
		if (a.getId() == null || a.getId().isEmpty()) {
			createAssertion(a);
		} else {
			Assertion existingAssertion = findById(a.getId());
			checkAssertionAccess(existingAssertion);
			
			if (assertionToDelete(a)) {
				deleteAssertionFromOrcidRegistry(a.getId());
				deleteById(a.getId());
			} else {
				updateAssertion(a);
			}
		}
	}

	public void deleteById(String id) {
		Assertion assertion = findById(id);
		String assertionEmail = assertion.getEmail();

		assertionsRepository.deleteById(id);

		// Remove OrcidRecord if no other assertions exist for user
		List<Assertion> assertions = assertionsRepository.findByEmail(assertionEmail);
		if (assertions.isEmpty()) {
			deleteOrcidRecordByEmail(assertionEmail);
		}
	}

	public List<Assertion> findAssertionsByEmail(String email) {
		return assertionsRepository.findByEmail(email);
	}

	private boolean assertionToDelete(Assertion assertion) {
		return assertion.getId() != null && assertion.getAddedToORCID() == null
				&& assertion.getAffiliationSection() == null && assertion.getCreated() == null
				&& assertion.getDeletedFromORCID() == null && assertion.getDepartmentName() == null
				&& assertion.getDisambiguatedOrgId() == null && assertion.getDisambiguationSource() == null
				&& assertion.getEmail() == null && assertion.getEndDay() == null && assertion.getEndMonth() == null
				&& assertion.getEndYear() == null && assertion.getExternalId() == null
				&& assertion.getExternalIdType() == null && assertion.getExternalIdUrl() == null
				&& assertion.getLastModifiedBy() == null && assertion.getModified() == null
				&& assertion.getOrcidError() == null && assertion.getOrcidId() == null && assertion.getOrgCity() == null
				&& assertion.getOrgCity() == null && assertion.getOrgCountry() == null && assertion.getOrgName() == null
				&& assertion.getOrgRegion() == null && assertion.getOwnerId() == null && assertion.getPutCode() == null
				&& assertion.getRoleTitle() == null && assertion.getSalesforceId() == null
				&& assertion.getStartDay() == null && assertion.getStartMonth() == null
				&& assertion.getStartYear() == null;
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
		List<Assertion> assertionsToAdd = assertionsRepository.findAllToCreate();
		for (Assertion assertion : assertionsToAdd) {
			postAssertionToOrcid(assertion);
		}
	}

	public void postAssertionToOrcid(Assertion assertion) throws JAXBException {
		Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(assertion.getEmail());
		if (!optional.isPresent()) {
			LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
			return;
		}
		OrcidRecord record = optional.get();
		String idToken = record.getToken(assertion.getSalesforceId());
		if (StringUtils.isBlank(record.getOrcid())) {
			LOG.warn("Orcid id still not available for {}", assertion.getEmail());
			return;
		}
		if (StringUtils.isBlank(idToken)) {
			LOG.warn("Id token still not available for {}", assertion.getEmail());
			return;
		}

		String orcid = record.getOrcid();
		String accessToken = null;
		try {
			accessToken = orcidAPIClient.exchangeToken(idToken);

			LOG.info("POST affiliation for {} and assertion id {}", orcid, assertion.getId());
			String putCode = orcidAPIClient.postAffiliation(orcid, accessToken, assertion);
			Optional<Assertion> existentAssertion = assertionsRepository.findById(assertion.getId());
			if (existentAssertion.isPresent() && StringUtils.isBlank(existentAssertion.get().getPutCode())) {
				assertion.setPutCode(putCode);
				Instant now = Instant.now();
				assertion.setAddedToORCID(now);
				assertion.setUpdatedInORCID(now);
				// assertion.setModified(now);
				assertion.setUpdated(false);
				// Remove error if any
				assertion.setOrcidError(null);
				assertion.setStatus(getAssertionStatus(assertion));
				assertionsRepository.save(assertion);
			}
		} catch (ORCIDAPIException oae) {
			storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
		} catch (Exception e) {
			LOG.error("Error with assertion " + assertion.getId(), e);
			storeError(assertion.getId(), 0, e.getMessage());
		}

	}

	public void putAssertionsToOrcid() throws JAXBException {
		List<Assertion> assertionsToUpdate = assertionsRepository.findAllToUpdate();
		for (Assertion assertion : assertionsToUpdate) {
			putAssertionToOrcid(assertion);
		}
	}

	public void putAssertionToOrcid(Assertion assertion) throws JAXBException {
		Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(assertion.getEmail());
		if (!optional.isPresent()) {
			LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
			return;
		}
		OrcidRecord record = optional.get();
		String idToken = record.getToken(assertion.getSalesforceId());
		if (StringUtils.isBlank(record.getOrcid())) {
			LOG.warn("Orcid id still not available for {}", assertion.getEmail());
			return;
		}
		if (StringUtils.isBlank(idToken)) {
			LOG.warn("Id token still not available for {}", assertion.getEmail());
			return;
		}
		String orcid = record.getOrcid();
		String accessToken = null;
		try {
			if (!StringUtils.isBlank(assertion.getPutCode())) {
				accessToken = orcidAPIClient.exchangeToken(idToken);
				LOG.info("PUT affiliation with put-code {} for {} and assertion id {}", assertion.getPutCode(), orcid,
						assertion.getId());
				orcidAPIClient.putAffiliation(orcid, accessToken, assertion);
				Instant now = Instant.now();
				assertion.setUpdatedInORCID(now);
				// assertion.setModified(now);
				assertion.setUpdated(false);
				// Remove error if any
				assertion.setOrcidError(null);
				assertion.setStatus(getAssertionStatus(assertion));
				assertionsRepository.save(assertion);
			} else {
				LOG.error("Error with assertion " + assertion.getId() + " cannot update it with putcode empty.");
			}
		} catch (ORCIDAPIException oae) {
			storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
		} catch (Exception e) {
			LOG.error("Error with assertion " + assertion.getId(), e);
			storeError(assertion.getId(), 0, e.getMessage());
		}
	}

	public boolean deleteAssertionFromOrcidRegistry(String assertionId) {
		Assertion assertion = assertionsRepository.findById(assertionId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid assertion id"));
		String salesForceId = assertionsUserService.getLoggedInUserSalesforceId();

		if (!salesForceId.equals(assertion.getSalesforceId())) {
			throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation",
					"affiliationOtherOrganization");
		}

		Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(assertion.getEmail());
		if (canDeleteAssertionFromOrcidRegistry(record, assertion)) {
			LOG.info("Exchanging id token for {}", record.get().getOrcid());
			try {
				String accessToken = orcidAPIClient.exchangeToken(record.get().getToken(assertion.getSalesforceId()));
				Boolean deleted = orcidAPIClient.deleteAffiliation(record.get().getOrcid(), accessToken, assertion);
				if (deleted) {
					Instant now = Instant.now();
					assertion.setDeletedFromORCID(now);
					assertion.setModified(now);
					assertion.setStatus(getAssertionStatus(assertion));
					assertionsRepository.save(assertion);
				}
				return deleted;
			} catch (ORCIDAPIException oae) {
				storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
			} catch (Exception e) {
				LOG.error("Error with assertion " + assertion.getId(), e);
				storeError(assertion.getId(), 0, e.getMessage());
			}
		}
		return false;
	}

	public String generateAssertionsReport() throws IOException {
		return assertionsReportCsvWriter.writeCsv();
	}

	private boolean canDeleteAssertionFromOrcidRegistry(Optional<OrcidRecord> record, Assertion assertion) {
		if (!record.isPresent()) {
			LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
			return false;
		}
		if (StringUtils.isBlank(record.get().getOrcid())) {
			LOG.warn("Orcid ID not available for {}", assertion.getEmail());
			return false;
		}
		if (record.get().getTokens() == null) {
			LOG.warn("Tokens not available for {}", assertion.getEmail());
			return false;
		}
		return true;
	}

	private void storeError(String assertionId, int statusCode, String error) {
		Assertion assertion = assertionsRepository.findById(assertionId)
				.orElseThrow(() -> new RuntimeException("Unable to find assertion with ID: " + assertionId));
		JSONObject obj = new JSONObject();
		obj.put("statusCode", statusCode);
		obj.put("error", error);
		assertion.setOrcidError(obj.toString());
		assertion.setUpdated(false);
		assertion.setStatus(getAssertionStatus(assertion));
		// get status text
		if (StringUtils.equals(assertion.getStatus(), AssertionStatus.USER_REVOKED_ACCESS.getValue())) {
			orcidRecordService.deleteIdToken(assertion.getEmail(), assertion.getSalesforceId());
		}
		assertionsRepository.save(assertion);
	}

	public String getAssertionStatus(Assertion assertion) {
		Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(assertion.getEmail());
		if (!optionalRecord.isPresent()) {
			throw new IllegalArgumentException("Found assertion with no corresponding record email - "
					+ assertion.getEmail() + " - " + assertion.getEmail());
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
		return assertionsRepository.findByEmail(email);
	}

	public List<Assertion> findByEmailAndSalesForceId(String email, String salesForceId) {
		return assertionsRepository.findByEmailAndSalesforceId(email, salesForceId);
	}

	private void deleteOrcidRecordByEmail(String email) {
		Optional<OrcidRecord> orcidRecordOptional = orcidRecordService.findOneByEmail(email);
		if (orcidRecordOptional.isPresent()) {
			OrcidRecord orcidRecord = orcidRecordOptional.get();
			orcidRecordService.deleteOrcidRecord(orcidRecord);
		}
	}

	public Optional<Assertion> findOneByEmailIgnoreCase(String email) {
		return assertionsRepository.findOneByEmailIgnoreCase(email.toLowerCase());
	}

	public List<Assertion> getAssertionsBySalesforceId(String salesforceId) {
		return assertionsRepository.findBySalesforceId(salesforceId);
	}

	public void assertionStatusCleanup() {
		List<Assertion> statusesToClean = assertionsRepository.findByStatus("");
		LOG.info("Found " + statusesToClean.size() + " assertion statuses to cleanup.");
		for (Assertion assertion : statusesToClean) {
			assertion.setStatus(getAssertionStatus(assertion));
			assertionsRepository.save(assertion);
		}
	}

	public void updateAssertionStatus(AssertionStatus status, Assertion assertion) {
		assertion.setStatus(status.getValue());
		assertionsRepository.save(assertion);
	}

	public String generatePermissionLinks() throws IOException {
		return permissionLinksCsvWriter.writeCsv();
	}

	public String generateAssertionsCSV() throws IOException {
		return assertionsForEditCsvWriter.writeCsv();
	}
}
