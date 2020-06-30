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
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.utils.AssertionUtils;
import org.orcid.repository.AssertionsRepository;
import org.orcid.service.assertions.report.impl.AssertionsCSVReportWriter;
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
	private AssertionsCSVReportWriter assertionsReportWriter;
	
	@Autowired
	private UserService assertionsUserService;

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
		Page<Assertion> assertionsPage = assertionsRepository.findByOwnerId(assertionsUserService.getLoggedInUserId(), pageable);
		assertionsPage.forEach(a -> { 
			a.setStatus(getAssertionStatus(a));
			
			if (a.getOrcidId() == null) {
				a.setOrcidId(getAssertionOrcidId(a));
			}
		});
		return assertionsPage;
	}

	public List<Assertion> findAllByOwnerId() {
		List<Assertion> assertions = assertionsRepository.findAllByOwnerId(assertionsUserService.getLoggedInUserId(), SORT);
		assertions.forEach(a -> { 
			a.setStatus(getAssertionStatus(a));
			
			if (a.getOrcidId() == null) {
				a.setOrcidId(getAssertionOrcidId(a));
			}
		});
		return assertions;
	}

	public Page<Assertion> findBySalesforceId(Pageable pageable) {
		AssertionServiceUser user = assertionsUserService.getLoggedInUser();
		Page<Assertion> assertions = assertionsRepository.findBySalesforceId(user.getSalesforceId(), pageable);
		assertions.forEach(a -> {
			a.setStatus(getAssertionStatus(a));
			
			if (a.getOrcidId() == null) {
				a.setOrcidId(getAssertionOrcidId(a));
			}
		});
		return assertions;
	}
	
        public void deleteAllBySalesforceId(String salesforceId) {
            List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesforceId, SORT);
            assertions.forEach(a -> {
                assertionsRepository.deleteById(a.getId());
            });
            return;
       }	

	public Assertion findById(String id) {
	        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
		Optional<Assertion> optional = assertionsRepository.findById(id);
		if (!optional.isPresent()) {
			throw new IllegalArgumentException("Invalid assertion id");
		}
		Assertion assertion = optional.get();
		if (!user.getSalesforceId().equals(assertion.getSalesforceId())) {
			throw new IllegalArgumentException(user.getId() + " doesn't belong to organization " + assertion.getSalesforceId());
		}
		assertion.setStatus(getAssertionStatus(assertion));
		if (assertion.getOrcidId() == null) {
			assertion.setOrcidId(getAssertionOrcidId(assertion));
		}
		return assertion;
	}

	public Assertion createAssertion(Assertion assertion) {
		Instant now = Instant.now();
		AssertionServiceUser user = assertionsUserService.getLoggedInUser();

		assertion.setOwnerId(user.getId());
		assertion.setCreated(now);
		assertion.setModified(now);
		assertion.setSalesforceId(user.getSalesforceId());

		String email = assertion.getEmail();

		Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(email);
		if (!optionalRecord.isPresent()) {
			orcidRecordService.createOrcidRecord(email, now);
		}

		assertion = assertionsRepository.insert(assertion);
		assertion.setStatus(getAssertionStatus(assertion));

		if (assertion.getOrcidId() == null) {
			assertion.setOrcidId(getAssertionOrcidId(assertion));
		}
		
		return assertion;
	}

	public void createAssertions(List<Assertion> assertions) {
		Instant now = Instant.now();
		String ownerId = assertionsUserService.getLoggedInUserId();
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
		String userId = assertionsUserService.getLoggedInUserId();
		Optional<Assertion> optional = assertionsRepository.findById(assertion.getId());
		Assertion existingAssertion = optional.get();

		if (!userId.equals(existingAssertion.getOwnerId())) {
			throw new IllegalArgumentException("Invalid assertion id");
		}

		copyFieldsToUpdate(assertion, existingAssertion);
		existingAssertion.setUpdated(true);
		existingAssertion.setModified(Instant.now());
		assertion = assertionsRepository.save(existingAssertion);
		assertion.setStatus(getAssertionStatus(assertion));
		
		if (assertion.getOrcidId() == null) {
			assertion.setOrcidId(getAssertionOrcidId(assertion));
		}
		return assertion;
	}

	public void deleteById(String id) {
		assertionsRepository.deleteById(id);
	}

	private void copyFieldsToUpdate(Assertion source, Assertion destination) {
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
		Map<String, String> accessTokens = new HashMap<String, String>();
		for (Assertion assertion : assertionsToAdd) {
			Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(assertion.getEmail());
			if (!optional.isPresent()) {
				LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
				continue;
			}
			OrcidRecord record = optional.get();
			if (StringUtils.isBlank(record.getOrcid())) {
				LOG.warn("Orcid id still not available for {}", assertion.getEmail());
				continue;
			}
			if (StringUtils.isBlank(record.getIdToken())) {
				LOG.warn("Id token still not available for {}", assertion.getEmail());
				continue;
			}

			String orcid = record.getOrcid();
			String idToken = record.getIdToken();
			try {
				String accessToken;
				if (accessTokens.containsKey(orcid)) {
					accessToken = accessTokens.get(orcid);
				} else {
					LOG.info("Exchanging id token for {}", orcid);
					accessToken = orcidAPIClient.exchangeToken(idToken);
					accessTokens.put(orcid, idToken);
				}

				LOG.info("POST affiliation for {} and assertion id {}", orcid, assertion.getId());
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
				LOG.error("Error with assertion " + assertion.getId(), e);
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
				LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
				continue;
			}
			OrcidRecord record = optional.get();
			if (StringUtils.isBlank(record.getOrcid())) {
				LOG.warn("Orcid id still not available for {}", assertion.getEmail());
				continue;
			}
			if (StringUtils.isBlank(record.getIdToken())) {
				LOG.warn("Id token still not available for {}", assertion.getEmail());
				continue;
			}

			String orcid = record.getOrcid();
			String idToken = record.getIdToken();
			try {
				String accessToken;
				if (accessTokens.containsKey(orcid)) {
					accessToken = accessTokens.get(orcid);
				} else {
					LOG.info("Exchanging id token for {}", orcid);
					accessToken = orcidAPIClient.exchangeToken(idToken);
					accessTokens.put(orcid, idToken);
				}
				LOG.info("PUT affiliation with put-code {} for {} and assertion id {}", assertion.getPutCode(), orcid,
						assertion.getId());
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
				LOG.error("Error with assertion " + assertion.getId(), e);
				storeError(assertion.getId(), 0, e.getMessage());
			}
		}
	}

	public boolean deleteAssertionFromOrcid(String assertionId) throws JSONException, JAXBException {
		Assertion assertion = assertionsRepository.findById(assertionId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid assertion id"));
		String userId = assertionsUserService.getLoggedInUserId();

		if (!userId.equals(assertion.getOwnerId())) {
			throw new IllegalArgumentException("Invalid assertion id");
		}

		Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(assertion.getEmail());
		if (!optional.isPresent()) {
			LOG.error("OrcidRecord not available for email {}", assertion.getEmail());
			return false;
		}
		OrcidRecord record = optional.get();
		if (StringUtils.isBlank(record.getOrcid())) {
			LOG.warn("Orcid id still not available for {}", assertion.getEmail());
			return false;
		}
		if (StringUtils.isBlank(record.getIdToken())) {
			LOG.warn("Id token still not available for {}", assertion.getEmail());
			return false;
		}

		LOG.info("Exchanging id token for {}", record.getOrcid());
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
			LOG.error("Error with assertion " + assertion.getId(), e);
			storeError(assertion.getId(), 0, e.getMessage());
		}
		return false;
	}

	public String generateAssertionsReport() throws IOException {
		return assertionsReportWriter.writeAssertionsReport();
	}

	private void storeError(String assertionId, int statusCode, String error) {
		Assertion assertion = assertionsRepository.findById(assertionId)
				.orElseThrow(() -> new RuntimeException("Unable to find assertion with ID: " + assertionId));
		JSONObject obj = new JSONObject();
		obj.put("statusCode", statusCode);
		obj.put("error", error);
		assertion.setOrcidError(obj.toString());
		assertionsRepository.save(assertion);
	}

	private String getAssertionStatus(Assertion assertion) {
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
			return !StringUtils.isBlank(record.getIdToken()) ? record.getOrcid() : null;
		}
		return null;
	}

	public List<Assertion> findByEmail(String email) {
		return assertionsRepository.findByEmail(email);
	}

}
