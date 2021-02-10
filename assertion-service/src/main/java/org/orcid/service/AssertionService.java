package org.orcid.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
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
import org.orcid.service.assertions.report.impl.AssertionsCSVReportWriter;
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

    private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

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
        String salesForceId = user.getSalesforceId();
        if(!StringUtils.isAllBlank(user.getLoginAs()))  {
            AssertionServiceUser loginAsUser = assertionsUserService.getLoginAsUser(user);
            salesForceId = loginAsUser.getSalesforceId();
        }
        
        Page<Assertion> assertions = assertionsRepository.findBySalesforceId(salesForceId , pageable);
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
            String assertionEmail = getAssertionEmail(a.getId());
            assertionsRepository.deleteById(a.getId());
            // Remove OrcidRecord if it has not already been removed
            Optional<OrcidRecord> orcidRecordOptional = orcidRecordService.findOneByEmail(assertionEmail);
            if (orcidRecordOptional.isPresent()) {
                deleteOrcidRecordByEmail(assertionEmail);
            }
        });
        return;
    }

    public void updateAssertionsSalesforceId(String salesforceId, String newSalesforceId) {
        List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesforceId, SORT);
        assertions.forEach(a -> {
            a.setSalesforceId(newSalesforceId);
            updateAssertionAsAdmin(a);
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
        String salesforceId = user.getSalesforceId();
        if(!StringUtils.isAllBlank(user.getLoginAs()))  {
            AssertionServiceUser loginAsUser = assertionsUserService.getLoginAsUser(user);
            salesforceId = loginAsUser.getSalesforceId();
        }
        if (!assertion.getSalesforceId().equals(salesforceId)) {
            throw new IllegalArgumentException(user.getId() + " doesn't belong to organization " + assertion.getSalesforceId());
        }
        assertion.setStatus(getAssertionStatus(assertion));
        if (assertion.getOrcidId() == null) {
            assertion.setOrcidId(getAssertionOrcidId(assertion));
        }
        if(assertion.getOrcidId() == null && StringUtils.equals(assertion.getStatus(), AssertionStatus.PENDING.value)) {
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
        if(!StringUtils.isAllBlank(user.getLoginAs()))  {
            AssertionServiceUser loginAsUser = assertionsUserService.getLoginAsUser(user);
            assertion.setSalesforceId(loginAsUser.getSalesforceId());
        } else {
            assertion.setSalesforceId(user.getSalesforceId());
        }

        String email = assertion.getEmail();

        Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(email);
        if (!optionalRecord.isPresent()) {
            orcidRecordService.createOrcidRecord(email, now, assertion.getSalesforceId());
        }
        else {
            OrcidRecord record = optionalRecord.get();
            List<OrcidToken> tokens = record.getTokens();
            boolean createToken = true;
            if(tokens == null || tokens.size() == 0) {
                tokens = new ArrayList<OrcidToken>();
                createToken = true;
            }
            else {
                for(OrcidToken token: tokens) {
                    if(StringUtils.equals(token.getSalesforce_id().trim(), assertion.getSalesforceId().trim())) {
                        createToken = false;
                        break;
                    }
                }
            }
          
            if(createToken) {
                tokens.add(new OrcidToken(assertion.getSalesforceId(), null));
                record.setTokens(tokens);
                record.setModified(Instant.now());
                orcidRecordService.updateOrcidRecord(record);
            }
            
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
            a.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
            // Create the assertion
            assertionsRepository.insert(a);
        }
    }

    public Assertion updateAssertion(Assertion assertion) {
        return updateAssertionImpl(assertion, false);
    }

    private Assertion updateAssertionAsAdmin(Assertion assertion) {
        return updateAssertionImpl(assertion, true);
    }

    private Assertion updateAssertionImpl(Assertion assertion, boolean updateAsAdmin) {
        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        String salesforceId = user.getSalesforceId();
        if(!StringUtils.isAllBlank(user.getLoginAs()))  {
            AssertionServiceUser loginAsUser = assertionsUserService.getLoginAsUser(user);
            salesforceId = loginAsUser.getSalesforceId();
        }
        
        Optional<Assertion> optional = assertionsRepository.findById(assertion.getId());
        Assertion existingAssertion = optional.get();

        if (!salesforceId.equals(existingAssertion.getSalesforceId()) && !updateAsAdmin) {
            throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation", "affiliationOtherOrganization");
        }

        if (assertion.getEmail() != null && existingAssertion.getEmail() != null && !assertion.getEmail().equals(existingAssertion.getEmail())) {
            updateEmailOrcidRecord(assertion.getEmail(), existingAssertion.getEmail());
        }
        copyFieldsToUpdate(assertion, existingAssertion);
        existingAssertion.setUpdated(true);
        existingAssertion.setModified(Instant.now());
        existingAssertion.setLastModifiedBy(user.getLogin());
        assertion = assertionsRepository.save(existingAssertion);
        assertion.setStatus(getAssertionStatus(assertion));

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

    public void deleteById(String id) {
        String assertionEmail = getAssertionEmail(id);
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

    private void copyFieldsToUpdate(Assertion source, Assertion destination) {
        destination.setEmail(source.getEmail());
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
                if(existentAssertion.isPresent() && StringUtils.isBlank(existentAssertion.get().getPutCode())) {
	                assertion.setPutCode(putCode);
	                Instant now = Instant.now();
	                assertion.setAddedToORCID(now);
	                assertion.setModified(now);
	                assertion.setUpdated(false);
	                // Remove error if any
	                assertion.setOrcidError(null);
	                assertionsRepository.save(assertion);
                }
            } catch (ORCIDAPIException oae) {
                storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
                if (oae.getError().contains("invalid_scope") || oae.getStatusCode() == 401) {
                    try
                    {
                      if(idToken !=null) {
                          removeIdTokenFromOrcidRecord(record, accessToken);
                      }
                    } catch (Exception ex){
                      LOG.error("Error with assertion when trying to remove token" + assertion.getId(), ex);
                    }
                }
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
            accessToken = orcidAPIClient.exchangeToken(idToken);
            LOG.info("PUT affiliation with put-code {} for {} and assertion id {}", assertion.getPutCode(), orcid, assertion.getId());
            orcidAPIClient.putAffiliation(orcid, accessToken, assertion);
            Instant now = Instant.now();
            assertion.setUpdatedInORCID(now);
            assertion.setModified(now);
            assertion.setUpdated(false);
            // Remove error if any
            assertion.setOrcidError(null);
            assertionsRepository.save(assertion);
        } catch (ORCIDAPIException oae) {
            storeError(assertion.getId(), oae.getStatusCode(), oae.getError());
            if (oae.getError().contains("invalid_scope") || oae.getStatusCode() == 401) {
                try
                {
                  if(idToken !=null) {
                      removeIdTokenFromOrcidRecord(record, idToken);
                  }
                } catch (Exception ex){
                  LOG.error("Error with assertion when trying to remove token" + assertion.getId(), ex);
                }
            }
        } catch (Exception e) {
            LOG.error("Error with assertion " + assertion.getId(), e);
            storeError(assertion.getId(), 0, e.getMessage());
        }
}

    public boolean deleteAssertionFromOrcid(String assertionId) throws JSONException, JAXBException {
        Assertion assertion = assertionsRepository.findById(assertionId).orElseThrow(() -> new IllegalArgumentException("Invalid assertion id"));
        
        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        String salesForceId;
        if(!StringUtils.isAllBlank(user.getLoginAs())) {
            AssertionServiceUser loginAsUser = assertionsUserService.getLoginAsUser(user);
            salesForceId = loginAsUser.getSalesforceId();
        } else {
            salesForceId = user.getSalesforceId();
        }

        if (!salesForceId.equals(assertion.getSalesforceId())) {
            throw new BadRequestAlertException("This affiliations doesnt belong to your organization", "affiliation", "affiliationOtherOrganization");
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
        if (record.getTokens() == null) {
            LOG.warn("Tokens still not available for {}", assertion.getEmail());
            return false;
        }
        else {
            
        }

        LOG.info("Exchanging id token for {}", record.getOrcid());
        String accessToken = null;
        try {
            accessToken = orcidAPIClient.exchangeToken(record.getToken(assertion.getSalesforceId()));

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
            if ((oae.getError().contains("invalid_scope") || oae.getStatusCode() == 401) && accessToken != null) {
                removeIdTokenFromOrcidRecord(record, accessToken);
            }
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
        Assertion assertion = assertionsRepository.findById(assertionId).orElseThrow(() -> new RuntimeException("Unable to find assertion with ID: " + assertionId));
        JSONObject obj = new JSONObject();
        obj.put("statusCode", statusCode);
        obj.put("error", error);
        assertion.setOrcidError(obj.toString());
        assertion.setUpdated(false);
        assertionsRepository.save(assertion);
    }

    private String getAssertionStatus(Assertion assertion) {
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
            if(StringUtils.isBlank(record.getToken(assertion.getSalesforceId()))) {
                return null;
            }
            return record.getOrcid();
        }
        return null;
    }

    private String getAssertionEmail(String assertionId) {
        Assertion assertion = assertionsRepository.findById(assertionId).orElseThrow(() -> new RuntimeException("Unable to find assertion with ID: " + assertionId));
        return assertion.getEmail();
    }

    public List<Assertion> findByEmail(String email) {
        return assertionsRepository.findByEmail(email);
    }

    private void updateEmailOrcidRecord(String newEmail, String oldEmail) {
        Optional<OrcidRecord> orcidRecordOptional = orcidRecordService.findOneByEmail(oldEmail);
        if (orcidRecordOptional.isPresent()) {
            OrcidRecord orcidRecord = orcidRecordOptional.get();
            orcidRecord.setEmail(newEmail);
            orcidRecord.setModified(Instant.now());
            orcidRecordService.updateOrcidRecord(orcidRecord);
        }
    }

    private void deleteOrcidRecordByEmail(String email) {
        Optional<OrcidRecord> orcidRecordOptional = orcidRecordService.findOneByEmail(email);
        if (orcidRecordOptional.isPresent()) {
            OrcidRecord orcidRecord = orcidRecordOptional.get();
            orcidRecordService.deleteOrcidRecord(orcidRecord);
        }
    }

    private void removeIdTokenFromOrcidRecord(OrcidRecord orcidRecord, String idToken) {
        if (orcidRecord != null && idToken!=null ) {
            List<OrcidToken> tokens = orcidRecord.getTokens();
            if(tokens !=null  && tokens.size() > 0) {
                List<OrcidToken> updatedTokens = new ArrayList<OrcidToken>();
                for(OrcidToken token: tokens)
                {   
                        if(!StringUtils.equals(token.getToken_id(), idToken)) {
                            updatedTokens.add(token);
                        }
                    
                }
                orcidRecord.setTokens(updatedTokens);
                orcidRecord.setModified(Instant.now());
                orcidRecordService.updateOrcidRecord(orcidRecord);
            }
        }
    }

    public Optional<Assertion> findOneByEmailIgnoreCase(String email) {
        return assertionsRepository.findOneByEmailIgnoreCase(email.toLowerCase());
    }

    public List<Assertion> getAssertionsBySalesforceId(String salesforceId) {
        return assertionsRepository.findBySalesforceId(salesforceId);
    }

}
