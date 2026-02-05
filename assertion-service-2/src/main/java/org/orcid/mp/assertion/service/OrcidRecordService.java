package org.orcid.mp.assertion.service;

import org.apache.commons.lang3.StringUtils;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.domain.OrcidToken;
import org.orcid.mp.assertion.repository.OrcidRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class OrcidRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(OrcidRecordService.class);

    @Autowired
    private OrcidRecordRepository orcidRecordRepository;

    public Optional<OrcidRecord> findByEmail(String email) {
        return orcidRecordRepository.findOneByEmail(email);
    }

    public void revokeIdToken(String email, String salesForceId) {
        LOG.info("Revoking id token for email {}, salesforce id {}", email, salesForceId);
        OrcidRecord orcidRecord = orcidRecordRepository.findOneByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find userInfo for email: " + email));

        Instant now = Instant.now();
        List<OrcidToken> tokens = orcidRecord.getTokens();
        if (tokens != null && !tokens.isEmpty()) {
            for (OrcidToken token : tokens) {
                if (StringUtils.equals(token.getSalesforceId(), salesForceId)) {
                    token.setRevokedDate(Instant.now());
                    break;
                }
            }
        }
        orcidRecord.setModified(now);
        orcidRecordRepository.save(orcidRecord);
    }

    public void deleteOrcidRecordByEmail(String email) {
        Optional<OrcidRecord> orcidRecordOptional = findByEmail(email);
        if (orcidRecordOptional.isPresent()) {
            OrcidRecord orcidRecord = orcidRecordOptional.get();
            deleteOrcidRecord(orcidRecord);
        }
    }

    public void deleteOrcidRecord(OrcidRecord orcidRecord) {
        orcidRecordRepository.delete(orcidRecord);
    }
}
