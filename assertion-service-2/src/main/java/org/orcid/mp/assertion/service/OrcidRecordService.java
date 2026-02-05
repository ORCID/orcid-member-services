package org.orcid.mp.assertion.service;

import org.apache.commons.lang3.StringUtils;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.domain.OrcidToken;
import org.orcid.mp.assertion.error.BadRequestAlertException;
import org.orcid.mp.assertion.repository.OrcidRecordRepository;
import org.orcid.mp.assertion.security.EncryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrcidRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(OrcidRecordService.class);

    @Autowired
    private OrcidRecordRepository orcidRecordRepository;

    @Autowired
    private EncryptUtil encryptUtil;

    @Value("${application.landingPageUrl}")
    private String landingPageUrl;

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

    public List<OrcidRecord> getRecordsWithoutTokens(String salesForceId) {
        return orcidRecordRepository.findAllToInvite(salesForceId);
    }

    public OrcidRecord createOrcidRecord(String email, Instant now, String salesForceId) {
        Optional<OrcidRecord> optional = findByEmail(email);
        if (optional.isPresent()) {
            throw new BadRequestAlertException("An Orcid Record with the email: " + email + " already exists.");
        }

        OrcidRecord or = new OrcidRecord();
        or.setEmail(email);
        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        tokens.add(new OrcidToken(salesForceId, null));
        or.setTokens(tokens);
        or.setCreated(now);
        or.setModified(now);

        return orcidRecordRepository.insert(or);
    }

    public OrcidRecord updateOrcidRecord(OrcidRecord orcidRecord) {
        return orcidRecordRepository.save(orcidRecord);
    }

    public void deleteOrcidRecordTokenByEmailAndSalesforceId(String email, String salesforceId) {
        Optional<OrcidRecord> orcidRecordOptional = findByEmail(email);
        if (orcidRecordOptional.isPresent()) {
            OrcidRecord orcidRecord = orcidRecordOptional.get();
            if (orcidRecord.getTokens() != null) {
                List<OrcidToken> updated = new ArrayList<>();
                for (OrcidToken token : orcidRecord.getTokens()) {
                    if (!StringUtils.equals(token.getSalesforceId(), salesforceId)) {
                        updated.add(token);
                    }
                }
                orcidRecord.setTokens(updated);
            }
            orcidRecordRepository.save(orcidRecord);

            if (orcidRecord.getTokens() == null || orcidRecord.getTokens().isEmpty()) {
                deleteOrcidRecordByEmail(email);
            }
        }
    }

    public String generateLinkForEmailAndSalesforceId(String email, String salesforceId) {
        Optional<OrcidRecord> record = orcidRecordRepository.findOneByEmail(email);
        if (!record.isPresent()) {
            createOrcidRecord(email, Instant.now(), salesforceId);
        }
        return landingPageUrl + "?state=" + encryptUtil.encrypt(salesforceId + "&&" + email);
    }

    public boolean userHasGrantedOrDeniedPermission(String email, String salesforceId) {
        Optional<OrcidRecord> orcidRecordOptional = findByEmail(email);
        if (orcidRecordOptional.isEmpty()) {
            return false;
        }

        return !StringUtils.isBlank(orcidRecordOptional.get().getToken(salesforceId, true));
    }


}
