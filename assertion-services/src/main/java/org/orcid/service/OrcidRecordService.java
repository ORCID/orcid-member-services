package org.orcid.service;

import java.time.Instant;
import java.util.Optional;

import org.orcid.domain.OrcidRecord;
import org.orcid.repository.OrcidRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrcidRecordService {

    @Autowired
    private OrcidRecordRepository orcidRecordRepository;
    
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
}
