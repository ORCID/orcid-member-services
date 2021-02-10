package org.orcid.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.orcid.config.ApplicationProperties;
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.repository.OrcidRecordRepository;
import org.orcid.security.EncryptUtil;
import org.orcid.security.SecurityUtils;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.jhipster.web.util.PaginationUtil;

@Service
public class OrcidRecordService {
    private static final Logger LOG = LoggerFactory.getLogger(OrcidRecordService.class);

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
    
    public OrcidRecord createOrcidRecord(String email, Instant now, String salesForceId) {
        Optional<OrcidRecord> optional = findOneByEmail(email);
        if (optional.isPresent()) {
            throw new BadRequestAlertException("An Orcid Record with the email: " + email + " already exists.", "orcidRecord", "orcidRecordEmailUsed");
        }
        
        OrcidRecord or = new OrcidRecord();
        or.setEmail(email);
        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        tokens.add( new OrcidToken(salesForceId, null));
        or.setTokens(tokens);
        or.setCreated(now);
        or.setModified(now);

        return orcidRecordRepository.insert(or);
    }
    
    public void createOrcidRecords(Set<String> emails, String salesForceId) {
        Instant now = Instant.now();
        // Create assertions
        for (String e : emails) {
            if (!findOneByEmail(e).isPresent()) {
                createOrcidRecord(e, now, salesForceId);
            }
        }
    }
    
    public void deleteOrcidRecord(OrcidRecord orcidRecord) {
    	orcidRecordRepository.delete(orcidRecord);
    }
    
    public void storeIdToken(String emailInStatus, String idToken, String orcidIdInJWT, String salesForceId) {
        OrcidRecord orcidRecord = orcidRecordRepository.findOneByEmail(emailInStatus).orElseThrow(() -> new IllegalArgumentException("Unable to find userInfo for email: " + emailInStatus));
        List<OrcidToken> tokens = orcidRecord.getTokens();
        List<OrcidToken> updatedTokens = new ArrayList<OrcidToken>();
        OrcidToken newToken = new OrcidToken(salesForceId, idToken);
        if(tokens == null || tokens.size() == 0)
        {
            updatedTokens.add(newToken);
        }
        else {
            for(OrcidToken token: tokens)
            {   
                    if(StringUtils.equals(token.getSalesforce_id(), salesForceId)) {
                        updatedTokens.add(newToken);
                    }
                    else {
                        updatedTokens.add(token);
                    }              
            }     
        } 
        orcidRecord.setTokens(updatedTokens);
        orcidRecord.setModified(Instant.now());
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

        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        String salesForceId;
        if(!StringUtils.isAllBlank(user.getLoginAs())) {
            AssertionServiceUser loginAsUser = assertionsUserService.getLoginAsUser(user);
            salesForceId = loginAsUser.getSalesforceId();
        } else {
            salesForceId = user.getSalesforceId();
        }

        List<OrcidRecord> records =  orcidRecordRepository.findAllToInvite(salesForceId);



        for(OrcidRecord record : records) {
            String email = record.getEmail();
            String encrypted = encryptUtil.encrypt(salesForceId + "&&" + email);
            String link = landingPageUrl + "?state=" + encrypted;
            csvPrinter.printRecord(email, link);
        }
        
        csvPrinter.flush();
        csvPrinter.close();
        return buffer.toString();
    }
    
    public String generateLinkForEmail(String email) {
        String landingPageUrl = applicationProperties.getLandingPageUrl();

        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        String salesForceId;
        if(!StringUtils.isAllBlank(user.getLoginAs())) {
            AssertionServiceUser loginAsUser = assertionsUserService.getLoginAsUser(user);
            salesForceId = loginAsUser.getSalesforceId();
        } else {
            salesForceId = user.getSalesforceId();
        }
        Optional<OrcidRecord> record =  orcidRecordRepository.findOneByEmail(email);
        if(!record.isPresent()) {
        	createOrcidRecord(email, Instant.now(), salesForceId);
        }

        return landingPageUrl + "?state=" + encryptUtil.encrypt(salesForceId + "&&" + email);    
    }

    public OrcidRecord updateOrcidRecord(OrcidRecord orcidRecord) {
        return orcidRecordRepository.save(orcidRecord);
    }
    
    public Page<OrcidRecord> findBySalesforceId(Pageable pageable) {
        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        Page<OrcidRecord> orcidRecords = orcidRecordRepository.findBySalesforceId(user.getSalesforceId(), pageable);
        orcidRecords.forEach(a -> {
            if(a.getToken(user.getSalesforceId()) == null || StringUtils.isBlank(a.getToken(user.getSalesforceId()))) {
                a.setOrcid(null);
            }
        });
        return orcidRecords;
    }
    
    
    public OrcidRecord findById(String id) {
        AssertionServiceUser user = assertionsUserService.getLoggedInUser();
        Optional<OrcidRecord> optional = orcidRecordRepository.findById(id);
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Invalid assertion id");
        }
        OrcidRecord record = optional.get();
        if (StringUtils.isBlank(record.getToken(user.getSalesforceId()))) {
            record.setOrcid(null);
        }
        return record;
    }
    
    
}
