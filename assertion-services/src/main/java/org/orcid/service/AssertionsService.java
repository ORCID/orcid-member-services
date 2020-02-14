package org.orcid.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.orcid.client.OrcidAPIClient;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.repository.AssertionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AssertionsService {
    private final Logger log = LoggerFactory.getLogger(AssertionsService.class);

    @Autowired
    private AssertionsRepository assertionsRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;
    
    @Autowired
    private OrcidAPIClient orcidAPIClient;

    public Page<Assertion> findByOwnerId(String loggedInUserId, Pageable pageable) {
        return assertionsRepository.findByOwnerId(loggedInUserId, pageable);
    }

    public List<Assertion> findAllByOwnerId(String loggedInUserId) {
        return assertionsRepository.findAllByOwnerId(loggedInUserId);
    }

    public Optional<Assertion> findById(String id) {
        return assertionsRepository.findById(id);
    }

    public Assertion createAssertion(String loggedInUserId, Assertion assertion) {
        Instant now = Instant.now();

        assertion.setOwnerId(loggedInUserId);
        assertion.setCreated(now);
        assertion.setModified(now);

        String email = assertion.getEmail();

        Optional<OrcidRecord> optionalRecord = orcidRecordService.findOneByEmail(email);
        if (!optionalRecord.isPresent()) {
            orcidRecordService.createOrcidRecord(email, loggedInUserId, now);
        }

        return assertionsRepository.save(assertion);
    }

    public void createAssertions(String loggedInUserId, List<Assertion> assertions) {
        Instant now = Instant.now();
        // Create assertions
        for (Assertion a : assertions) {
            a.setOwnerId(loggedInUserId);
            a.setCreated(now);
            a.setModified(now);
            // Create the assertion
            assertionsRepository.insert(a);
        }
    }

    public Assertion updateAssertion(String loggedInUserId, Assertion assertion) {
        Optional<Assertion> optional = assertionsRepository.findById(assertion.getId());
        Assertion existingAssertion = optional.get();

        if (!loggedInUserId.equals(existingAssertion.getOwnerId())) {
            throw new IllegalArgumentException("Invalid assertion id");
        }

        copyFieldsToUpdate(assertion, existingAssertion);
        assertion.setUpdated(true);
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
    }

    public void pushAssertionsToOrcid() throws JAXBException  {
        List<Assertion> assertionsToAdd = assertionsRepository.findAllToCreate();
        Map<String, String> accessTokens = new HashMap<String, String>();
        // TODO: What we will do if the user revoke permissions?
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
            String accessToken;
            if(accessTokens.containsKey(orcid)) {
                accessToken = accessTokens.get(orcid);
            } else {
                log.info("Exchanging id token for {}", orcid);
                try {
                    accessToken = orcidAPIClient.exchangeToken(idToken);
                } catch(Exception e) {
                    log.error("Unable to exchange id token for " + orcid, e);                    
                    break;
                }
                accessTokens.put(orcid, idToken);
            }
            
            log.info("POST affiliation for {} and assertion id {}", orcid, assertion.getId());
            String putCode = orcidAPIClient.postAffiliation(orcid, accessToken, assertion);
            assertion.setPutCode(putCode);
            assertion.setAddedToORCID(Instant.now());
            assertionsRepository.save(assertion);
        }
    }

}
