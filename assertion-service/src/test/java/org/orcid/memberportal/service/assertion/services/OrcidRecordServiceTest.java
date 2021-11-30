package org.orcid.memberportal.service.assertion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.memberportal.service.assertion.client.OrcidAPIClient;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.OrcidToken;
import org.orcid.memberportal.service.assertion.repository.OrcidRecordRepository;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.orcid.memberportal.service.assertion.services.UserService;
import org.orcid.memberportal.service.assertion.web.rest.errors.BadRequestAlertException;

class OrcidRecordServiceTest {

    private static final String DEFAULT_JHI_USER_ID = "user-id";

    private static final String DEFAULT_LOGIN = "user@orcid.org";

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

    private static final String OTHER_SALESFORCE_ID = "other-salesforce-id";

    private static final String EMAIL_ONE = "emailone@record.com";

    private static final String EMAIL_NO_TOKEN = "email_no_token@record.com";

    @InjectMocks
    private OrcidRecordService orcidRecordService;

    @Mock
    private OrcidRecordRepository orcidRecordRepository;

    @Mock
    private OrcidAPIClient orcidAPIClient;

    @Mock
    private UserService assertionsUserService;

    @BeforeEach
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);
        when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
        when(assertionsUserService.getLoggedInUserId()).thenReturn(getUser().getId());
    }

    private AssertionServiceUser getUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setId(DEFAULT_JHI_USER_ID);
        user.setEmail(DEFAULT_LOGIN);
        user.setSalesforceId(DEFAULT_SALESFORCE_ID);
        return user;
    }

    @Test
    void testCreateOrcidRecord() {
        Mockito.when(orcidRecordRepository.findOneByEmail(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(orcidRecordRepository.save(Mockito.any(OrcidRecord.class))).thenAnswer(new Answer<OrcidRecord>() {
            @Override
            public OrcidRecord answer(InvocationOnMock invocation) throws Throwable {
                return (OrcidRecord) invocation.getArgument(0);
            }
        });
        Mockito.when(orcidRecordRepository.insert(Mockito.any(OrcidRecord.class))).thenAnswer(new Answer<OrcidRecord>() {
            @Override
            public OrcidRecord answer(InvocationOnMock invocation) throws Throwable {
                return (OrcidRecord) invocation.getArgument(0);
            }
        });

        OrcidRecord created = orcidRecordService.createOrcidRecord(EMAIL_ONE, Instant.now(), DEFAULT_SALESFORCE_ID);
        assertNotNull(created.getCreated());
        assertNotNull(created.getModified());
        assertEquals(EMAIL_ONE, created.getEmail());

        List<OrcidToken> tokens = created.getTokens();
        OrcidToken token = tokens.get(0);

        assertEquals(DEFAULT_SALESFORCE_ID, token.getSalesforceId());
        assertEquals(null, token.getTokenId());

    }

    @Test
    void testCreateOrcidRecordWhenEmailExists() {
        Mockito.when(orcidRecordRepository.findOneByEmail(Mockito.anyString())).thenReturn(Optional.of(getOrcidRecordWithIdToken(EMAIL_ONE)));
        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            orcidRecordService.createOrcidRecord(EMAIL_ONE, Instant.now(), DEFAULT_SALESFORCE_ID);
        });
    }

    @Test
    void testUpdateOrcidRecord() {
        Mockito.when(orcidRecordRepository.findOneByEmail(Mockito.anyString())).thenReturn(Optional.of(getOrcidRecordWithIdToken(EMAIL_ONE)));
        Mockito.when(orcidRecordRepository.save(Mockito.any(OrcidRecord.class))).thenAnswer(new Answer<OrcidRecord>() {
            @Override
            public OrcidRecord answer(InvocationOnMock invocation) throws Throwable {
                return (OrcidRecord) invocation.getArgument(0);
            }
        });

        OrcidRecord recordOne = getOrcidRecordWithIdToken(EMAIL_ONE);
        recordOne.setId("xyz");
        List<OrcidToken> tokens = recordOne.getTokens();
        OrcidToken token = tokens.get(0);
        OrcidToken token2 = new OrcidToken(OTHER_SALESFORCE_ID, "tokenid2", null, null);
        tokens.add(token2);
        recordOne.setTokens(tokens);
        recordOne.setModified(Instant.now());

        OrcidRecord updated = orcidRecordService.updateOrcidRecord(recordOne);
        assertNotNull(updated.getCreated());
        assertNotNull(updated.getModified());

        tokens = updated.getTokens();
        token = tokens.get(1);
        assertEquals(OTHER_SALESFORCE_ID, token.getSalesforceId());
        assertEquals("tokenid2", token.getTokenId());
    }

    /*
     * @Test void testUpdateNonExistentOrcidRecord() {
     * Mockito.when(orcidRecordRepository.findOneByEmail(Mockito.anyString())).
     * thenReturn(Optional.of(getOrcidRecordWithIdToken(EMAIL_ONE)));
     * Mockito.when(orcidRecordRepository.save(Mockito.any(OrcidRecord.class))).
     * thenAnswer(new Answer<OrcidRecord>() {
     * 
     * @Override public OrcidRecord answer(InvocationOnMock invocation) throws
     * Throwable { return (OrcidRecord) invocation.getArgument(0); } });
     * 
     * OrcidRecord recordOne = getOrcidRecordWithIdToken(EMAIL_ONE);
     * recordOne.setId("xyze");
     * Assertions.assertThrows(BadRequestAlertException.class, () -> {
     * orcidRecordService.updateOrcidRecord(recordOne); }); }
     */

    private OrcidRecord getOrcidRecordWithIdToken(String email) {
        OrcidRecord record = new OrcidRecord();
        record.setCreated(Instant.now());
        record.setEmail(email);
        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken", null, null);
        tokens.add(newToken);
        record.setTokens(tokens);
        record.setId("xyz");
        record.setOrcid("orcid");
        return record;
    }

}
