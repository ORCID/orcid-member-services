package org.orcid.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.orcid.config.ApplicationProperties;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.repository.OrcidRecordRepository;
import org.orcid.security.EncryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrcidRecordService {

    @Autowired
    private OrcidRecordRepository orcidRecordRepository;
    
    @Autowired
    private EncryptUtil encryptUtil;
    
    @Autowired
    private ApplicationProperties applicationProperties;
    
    @Autowired
    private UserService assertionsUserService;
    
    public Optional<OrcidRecord> findOneByEmail(String email) {
        return orcidRecordRepository.findOneByEmail(email);
    }
    
    public void createOrcidRecord(String email, Instant now) {
        OrcidRecord or = new OrcidRecord();
        or.setEmail(email);
        or.setOwnerId(assertionsUserService.getLoggedInUserId());
        or.setCreated(now);
        or.setModified(now);
        orcidRecordRepository.insert(or);
    }
    
    public void createOrcidRecords(Set<String> emails) {
        Instant now = Instant.now();
        // Create assertions
        for (String e : emails) {
            if (!findOneByEmail(e).isPresent()) {
                createOrcidRecord(e, now);
            }
        }
    }
    
    public void deleteOrcidRecord(OrcidRecord orcidRecord) {
    	orcidRecordRepository.delete(orcidRecord);
    }
    
    public void storeIdToken(String emailInStatus, String idToken, String orcidIdInJWT) {
        OrcidRecord orcidRecord = orcidRecordRepository.findOneByEmail(emailInStatus).orElseThrow(() -> new IllegalArgumentException("Unable to find userInfo for email: " + emailInStatus));
        orcidRecord.setIdToken(idToken);
        orcidRecord.setOrcid(orcidIdInJWT);
        orcidRecord.setRevokeNotificationSentDate(null);
        orcidRecordRepository.save(orcidRecord);
    }
    
    public void storeUserDeniedAccess(String emailInStatus) {
        OrcidRecord orcidRecord = orcidRecordRepository.findOneByEmail(emailInStatus).orElseThrow(() -> new IllegalArgumentException("Unable to find userInfo for email: " + emailInStatus));
        orcidRecord.setDeniedDate(Instant.now());
        orcidRecordRepository.save(orcidRecord);
    }
    
    public String generateLinks() throws IOException {
        String landingPageUrl = applicationProperties.getLandingPageUrl();
        StringBuffer buffer = new StringBuffer();
        CSVPrinter csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT
                .withHeader("email", "link"));
        List<OrcidRecord> records = null;
                
        List<AssertionServiceUser> usersBelongingToSameMember = assertionsUserService.getUsersBySalesforceId(assertionsUserService.getLoggedInUser().getSalesforceId());
        for (AssertionServiceUser user : usersBelongingToSameMember) {
            if (records == null)
            {
                records = orcidRecordRepository.findAllToInvite(user.getId());
            }
            else
            {
                records.addAll(orcidRecordRepository.findAllToInvite(user.getId())); 
            }
        }       
        
        for(OrcidRecord record : records) {
            String email = record.getEmail();
            String encrypted = encryptUtil.encrypt(email);
            String link = landingPageUrl + "?state=" + encrypted;
            csvPrinter.printRecord(email, link);
        }
        
        csvPrinter.flush();
        csvPrinter.close();
        return buffer.toString();
    }

    public void updateOrcidRecord(OrcidRecord orcidRecord) {
        orcidRecordRepository.save(orcidRecord);
    }
}
