package org.orcid.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.orcid.config.ApplicationProperties;
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
    
    public Optional<OrcidRecord> findOneByEmail(String email) {
        return orcidRecordRepository.findOneByEmail(email);
    }
    
    public void createOrcidRecord(String email, String ownerId, Instant now) {
        OrcidRecord or = new OrcidRecord();
        or.setEmail(email);
        or.setOwnerId(ownerId);
        or.setCreated(now);
        or.setModified(now);
        orcidRecordRepository.insert(or);
    }
    
    public void createOrcidRecords(String ownerId, Set<String> emails) {
        Instant now = Instant.now();
        // Create assertions
        for (String e : emails) {
            if (!findOneByEmail(e).isPresent()) {
                createOrcidRecord(e, ownerId, now);
            }
        }
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
    
    public String generateLinks(String currentUser) throws IOException {
        String landingPageUrl = applicationProperties.getLandingPageUrl();
        StringBuffer buffer = new StringBuffer();
        CSVPrinter csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT
                .withHeader("email", "link"));
        List<OrcidRecord> records = orcidRecordRepository.findAllToInvite(currentUser);
        
        for(OrcidRecord record : records) {
            String email = record.getEmail();
            String encrypted = encryptUtil.encrypt(email);
            String link = landingPageUrl + '/' + encrypted;
            csvPrinter.printRecord(email, link);
        }
        
        csvPrinter.flush();
        csvPrinter.close();
        return buffer.toString();
    }
}
