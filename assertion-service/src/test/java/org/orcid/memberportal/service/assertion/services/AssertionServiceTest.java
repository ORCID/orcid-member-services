package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.http.client.ClientProtocolException;
import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.memberportal.service.assertion.client.OrcidAPIClient;
import org.orcid.memberportal.service.assertion.csv.download.impl.AssertionsForEditCsvWriter;
import org.orcid.memberportal.service.assertion.csv.download.impl.PermissionLinksCsvWriter;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.CsvReport;
import org.orcid.memberportal.service.assertion.domain.MemberAssertionStatusCount;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.OrcidToken;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.domain.normalization.AssertionNormalizer;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.upload.AssertionsUpload;
import org.orcid.memberportal.service.assertion.upload.AssertionsUploadSummary;
import org.orcid.memberportal.service.assertion.upload.impl.AssertionsCsvReader;
import org.orcid.memberportal.service.assertion.web.rest.errors.DeactivatedException;
import org.orcid.memberportal.service.assertion.web.rest.errors.DeprecatedException;
import org.orcid.memberportal.service.assertion.web.rest.errors.ORCIDAPIException;
import org.orcid.memberportal.service.assertion.web.rest.errors.RegistryDeleteFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

class AssertionServiceTest {

    private static final String DEFAULT_JHI_USER_ID = "user-id";

    private static final String DEFAULT_LOGIN = "user@orcid.org";

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

    @Mock
    private CsvReportService csvReportService;

    @Mock
    private AssertionsForEditCsvWriter assertionsForEditCsvWriter;

    @Mock
    private PermissionLinksCsvWriter permissionLinksCsvWriter;

    @Mock
    private AssertionRepository assertionRepository;

    @Mock
    private OrcidRecordService orcidRecordService;

    @Mock
    private OrcidAPIClient orcidAPIClient;

    @Mock
    private UserService assertionsUserService;

    @Mock
    private AssertionsCsvReader assertionsCsvReader;

    @Mock
    private AssertionNormalizer assertionNormalizer;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private MailService mailService;

    @Mock
    private MemberService memberService;

    @Captor
    private ArgumentCaptor<Assertion> assertionCaptor;

    @Captor
    private ArgumentCaptor<String> csvContentCaptor;

    @Captor
    private ArgumentCaptor<AssertionsUploadSummary> summaryCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Captor
    private ArgumentCaptor<StoredFile> storedFileCaptor;

    @Captor
    private ArgumentCaptor<String> filenameCaptor;

    @InjectMocks
    private AssertionService assertionService;

    @BeforeEach
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);
        when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
        when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
        when(assertionNormalizer.normalize(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0);
            }
        });
    }

    private AssertionServiceUser getUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setId(DEFAULT_JHI_USER_ID);
        user.setEmail(DEFAULT_LOGIN);
        user.setSalesforceId(DEFAULT_SALESFORCE_ID);
        user.setLangKey("en");
        return user;
    }

    @Test
    void testMarkPendingAssertionsAsNotificationRequested() {
        Mockito.doNothing().when(assertionRepository).updateStatusPendingToNotificationRequested(Mockito.eq("salesforce"));
        assertionService.markPendingAssertionsAsNotificationRequested("salesforce");
        Mockito.verify(assertionRepository).updateStatusPendingToNotificationRequested(Mockito.eq("salesforce"));
    }

    @Test
    void testUpdateOrcidIdsForEmailAndSalesforceId() {
        OrcidRecord record = new OrcidRecord();
        record.setOrcid("orcid");

        Assertion one = new Assertion();
        one.setSalesforceId(DEFAULT_SALESFORCE_ID);
        one.setRoleTitle("this one sould be saved");

        Assertion two = new Assertion();
        two.setSalesforceId("something from another org");
        two.setRoleTitle("this shouldn't be saved");

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.of(record));
        Mockito.when(assertionRepository.findAllByEmail(Mockito.eq("email"))).thenReturn(Arrays.asList(one, two));

        assertionService.updateOrcidIdsForEmailAndSalesforceId("email", DEFAULT_SALESFORCE_ID);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());

        Assertion captured = assertionCaptor.getValue();
        assertEquals("this one sould be saved", captured.getRoleTitle());
        assertNotNull(captured.getOrcidId());
        assertEquals("orcid", captured.getOrcidId());
    }

    @Test
    void testPopulatePermissionLink() {
        Assertion assertion = getAssertionWithEmail("email");
        Mockito.when(orcidRecordService.generateLinkForEmail(Mockito.eq("email"))).thenReturn("permission-link");
        assertionService.populatePermissionLink(assertion);
        assertEquals("permission-link", assertion.getPermissionLink());
        Mockito.verify(orcidRecordService).generateLinkForEmail(Mockito.eq("email"));
    }

    @Test
    void testAssertionExists() {
        when(assertionRepository.existsById(Mockito.eq("exists"))).thenReturn(true);
        when(assertionRepository.existsById(Mockito.eq("doesn't exist"))).thenReturn(false);

        assertTrue(assertionService.assertionExists("exists"));
        verify(assertionRepository).existsById(Mockito.eq("exists"));

        assertFalse(assertionService.assertionExists("doesn't exist"));
        verify(assertionRepository).existsById(Mockito.eq("doesn't exist"));
    }

    @Test
    void testCreateAssertion() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOwnerId(DEFAULT_JHI_USER_ID);
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getOptionalOrcidRecordWithIdToken());
        Mockito.when(assertionRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                assertion.setStatus("PENDING");
                return assertion;
            }
        });

        assertionService.createAssertion(a, getUser());
        Mockito.verify(assertionRepository, Mockito.times(1)).insert(assertionCaptor.capture());
        Mockito.verify(assertionNormalizer, Mockito.times(1)).normalize(Mockito.eq(a));

        Assertion inserted = assertionCaptor.getValue();
        assertEquals("orcid", inserted.getOrcidId());
    }

    @Test
    void testCreateAssertionNoIdToken() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOwnerId(DEFAULT_JHI_USER_ID);
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);

        OrcidRecord recordWithTokenPlaceholder = getOrcidRecord("1");
        recordWithTokenPlaceholder.setTokens(Arrays.asList(new OrcidToken(DEFAULT_SALESFORCE_ID, null)));

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.of(recordWithTokenPlaceholder));
        Mockito.when(assertionRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                return assertion;
            }
        });

        assertionService.createAssertion(a, getUser());
        Mockito.verify(assertionRepository, Mockito.times(1)).insert(assertionCaptor.capture());

        Assertion inserted = assertionCaptor.getValue();
        assertNull(inserted.getOrcidId());
    }

    @Test
    void testCreateAssertionNoOrcidRecord() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOwnerId(DEFAULT_JHI_USER_ID);

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.empty());

        Mockito.when(assertionRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                return assertion;
            }
        });

        assertionService.createAssertion(a, getUser());
        Mockito.verify(assertionRepository, Mockito.times(1)).insert(assertionCaptor.capture());

        Assertion inserted = assertionCaptor.getValue();
        assertNull(inserted.getOrcidId());
    }

    @Test
    void testUpdateAssertion_notPreviouslyAddedToRegistry() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOwnerId(DEFAULT_JHI_USER_ID);
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);
        Mockito.when(assertionRepository.findById("1")).thenReturn(Optional.of(a));
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                return assertion;
            }
        });
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        a = assertionService.updateAssertion(a, getUser());
        assertNotNull(a.getStatus());
        assertEquals(AssertionStatus.PENDING.name(), a.getStatus());
        Mockito.verify(assertionRepository, Mockito.times(1)).save(Mockito.eq(a));
        Mockito.verify(assertionNormalizer, Mockito.times(1)).normalize(Mockito.eq(a));
    }

    @Test
    void testUpdateAssertion_previouslyAddedToRegistry() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOwnerId(DEFAULT_JHI_USER_ID);
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);
        a.setAddedToORCID(Instant.now());
        Mockito.when(assertionRepository.findById("1")).thenReturn(Optional.of(a));
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                return assertion;
            }
        });
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        a = assertionService.updateAssertion(a, getUser());
        assertNotNull(a.getStatus());
        assertEquals(AssertionStatus.PENDING_UPDATE.name(), a.getStatus());
        Mockito.verify(assertionRepository, Mockito.times(1)).save(Mockito.eq(a));
        Mockito.verify(assertionNormalizer, Mockito.times(1)).normalize(Mockito.eq(a));
    }

    @Test
    void testUpdateAssertion_checkStatuses() {
        Assertion skeleton = new Assertion();
        skeleton.setId("1");
        skeleton.setEmail("email");
        skeleton.setOwnerId(DEFAULT_JHI_USER_ID);
        skeleton.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion full = new Assertion();
        full.setId("1");
        full.setEmail("email");
        full.setOwnerId(DEFAULT_JHI_USER_ID);
        full.setSalesforceId(DEFAULT_SALESFORCE_ID);

        full.setAddedToORCID(null);
        testUpdateStatus(skeleton, full, AssertionStatus.PENDING.name(), getOptionalOrcidRecordWithoutIdToken());

        full.setStatus(AssertionStatus.NOTIFICATION_SENT.name());
        testUpdateStatus(skeleton, full, AssertionStatus.NOTIFICATION_SENT.name(), getOptionalOrcidRecordWithoutIdToken());

        full.setStatus(AssertionStatus.NOTIFICATION_REQUESTED.name());
        testUpdateStatus(skeleton, full, AssertionStatus.NOTIFICATION_REQUESTED.name(), getOptionalOrcidRecordWithoutIdToken());

        full.setStatus(AssertionStatus.NOTIFICATION_FAILED.name());
        testUpdateStatus(skeleton, full, AssertionStatus.NOTIFICATION_FAILED.name(), getOptionalOrcidRecordWithoutIdToken());

        full.setStatus(AssertionStatus.ERROR_ADDING_TO_ORCID.name());
        testUpdateStatus(skeleton, full, AssertionStatus.PENDING_RETRY.name(), getOptionalOrcidRecordWithIdToken());

        full.setStatus(AssertionStatus.PENDING.name());
        testUpdateStatus(skeleton, full, AssertionStatus.PENDING.name(), getOptionalOrcidRecordWithIdToken());

        full.setAddedToORCID(Instant.now());
        full.setStatus(AssertionStatus.ERROR_UPDATING_TO_ORCID.name());
        testUpdateStatus(skeleton, full, AssertionStatus.PENDING_RETRY.name(), getOptionalOrcidRecordWithIdToken());

        full.setStatus(AssertionStatus.ERROR_DELETING_IN_ORCID.name());
        testUpdateStatus(skeleton, full, AssertionStatus.ERROR_DELETING_IN_ORCID.name(), getOptionalOrcidRecordWithIdToken());

        full.setStatus(AssertionStatus.IN_ORCID.name());
        testUpdateStatus(skeleton, full, AssertionStatus.PENDING_UPDATE.name(), getOptionalOrcidRecordWithIdToken());

        full.setStatus(AssertionStatus.PENDING_UPDATE.name());
        testUpdateStatus(skeleton, full, AssertionStatus.PENDING_UPDATE.name(), getOptionalOrcidRecordWithIdToken());

        testUpdateStatus(skeleton, full, AssertionStatus.USER_REVOKED_ACCESS.name(), Optional.of(getOrcidRecordWithRevokedToken()));
        testUpdateStatus(skeleton, full, AssertionStatus.USER_DENIED_ACCESS.name(), Optional.of(getOrcidRecordWithDeniedToken()));
    }

    @Test
    void testCreateAssertion_checkStatuses() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOwnerId(DEFAULT_JHI_USER_ID);
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);
        a.setAddedToORCID(null);

        testCreateStatus(a, AssertionStatus.PENDING.name(), getOptionalOrcidRecordWithoutIdToken());
        testCreateStatus(a, AssertionStatus.USER_REVOKED_ACCESS.name(), Optional.of(getOrcidRecordWithRevokedToken()));
        testCreateStatus(a, AssertionStatus.USER_DENIED_ACCESS.name(), Optional.of(getOrcidRecordWithDeniedToken()));
    }

    @Test
    void testUpdateAssertionNoIdToken() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOwnerId(DEFAULT_JHI_USER_ID);
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Mockito.when(assertionRepository.findById("1")).thenReturn(Optional.of(a));
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setStatus("PENDING");
                assertion.setId("12345");
                return assertion;
            }
        });
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithoutIdToken());
        a = assertionService.updateAssertion(a, getUser());
        assertNotNull(a.getStatus());
        Mockito.verify(assertionRepository, Mockito.times(1)).save(Mockito.eq(a));
    }

    @Test
    void testOrcidNotFetchedIfAlreadyPresent() {
        Assertion a = new Assertion();
        a.setId("1");
        a.setEmail("email");
        a.setOrcidId("orcid-already-present");
        a.setOwnerId(DEFAULT_JHI_USER_ID);
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);
        Mockito.when(assertionRepository.findById("1")).thenReturn(Optional.of(a));
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                assertion.setStatus("PENDING");
                return assertion;
            }
        });
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        a = assertionService.updateAssertion(a, getUser());
        assertNotNull(a.getStatus());
        assertEquals("orcid-already-present", a.getOrcidId());
        Mockito.verify(assertionRepository, Mockito.times(1)).save(Mockito.eq(a));
    }

    @Test
    void testPostAssertionsToOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        Mockito.when(assertionRepository.findAllToCreateInOrcidRegistry(Mockito.any(Pageable.class)))
            .thenReturn(getAssertionsForCreatingInOrcid(1, AssertionService.REGISTRY_SYNC_BATCH_SIZE))
            .thenReturn(getAssertionsForCreatingInOrcid(AssertionService.REGISTRY_SYNC_BATCH_SIZE + 1,
                AssertionService.REGISTRY_SYNC_BATCH_SIZE + (AssertionService.REGISTRY_SYNC_BATCH_SIZE / 2)))
            .thenReturn(new ArrayList<>());

        for (int i = 1; i <= 5; i++) {
            Mockito.when(orcidRecordService.findOneByEmail(i + "@email.com")).thenReturn(Optional.of(getOrcidRecord(Integer.toString(i))));
        }

        for (int i = 6; i <= AssertionService.REGISTRY_SYNC_BATCH_SIZE * 1.5; i++) {
            Mockito.when(orcidRecordService.findOneByEmail(i + "@email.com")).thenReturn(getOptionalOrcidRecord(i));
        }

        for (int i = 1; i <= 5; i++) {
            Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken" + i), Mockito.eq("orcid" + i))).thenReturn("accessToken" + i);
            Mockito.when(orcidAPIClient.postAffiliation(Mockito.eq("orcid" + i), Mockito.eq("accessToken" + i), Mockito.any(Assertion.class))).thenReturn("putCode" + i);
        }

        assertionService.postAssertionsToOrcid();

        Mockito.verify(orcidRecordService, Mockito.times(AssertionService.REGISTRY_SYNC_BATCH_SIZE + (AssertionService.REGISTRY_SYNC_BATCH_SIZE / 2)))
            .findOneByEmail(Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.times(5)).postAffiliation(Mockito.anyString(), Mockito.anyString(), assertionCaptor.capture());
        Mockito.verify(assertionRepository, Mockito.times(3)).findAllToCreateInOrcidRegistry(pageableCaptor.capture());

        List<Pageable> pageables = pageableCaptor.getAllValues();
        assertEquals(0, pageables.get(0).getPageNumber());
        assertEquals(AssertionService.REGISTRY_SYNC_BATCH_SIZE, pageables.get(0).getPageSize());

        assertEquals(1, pageables.get(1).getPageNumber());
        assertEquals(AssertionService.REGISTRY_SYNC_BATCH_SIZE, pageables.get(0).getPageSize());

        assertEquals(2, pageables.get(2).getPageNumber());
        assertEquals(AssertionService.REGISTRY_SYNC_BATCH_SIZE, pageables.get(0).getPageSize());

        List<Assertion> posted = assertionCaptor.getAllValues();
        posted.forEach(a -> {
            assertNotNull(a.getLastSyncAttempt());
            assertEquals(a.getLastSyncAttempt(), a.getAddedToORCID());
        });
    }

    @Test
    void testPostAssertionsToOrcidWithRevokedTokens() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeprecatedException {
        Mockito.when(assertionRepository.findAllToCreateInOrcidRegistry(Mockito.any(Pageable.class)))
            .thenReturn(getAssertionsForCreatingInOrcid(1, AssertionService.REGISTRY_SYNC_BATCH_SIZE)).thenReturn(new ArrayList<>());

        OrcidRecord recordWithRevokedToken = getOrcidRecordWithRevokedToken();
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.of(recordWithRevokedToken));

        assertionService.postAssertionsToOrcid();

        Mockito.verify(orcidRecordService, Mockito.times(AssertionService.REGISTRY_SYNC_BATCH_SIZE)).findOneByEmail(Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.never()).postAffiliation(Mockito.anyString(), Mockito.anyString(), assertionCaptor.capture());
        Mockito.verify(assertionRepository, Mockito.times(2)).findAllToCreateInOrcidRegistry(pageableCaptor.capture());
    }

    @Test
    void testPostAssertionsToOrcidWithDeactivatedProfile() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeprecatedException, DeactivatedException {
        List<Assertion> assertions = getAssertionsForCreatingInOrcid(1, 1);

        Mockito.when(assertionRepository.findAllToCreateInOrcidRegistry(Mockito.any(Pageable.class)))
            .thenReturn(assertions).thenReturn(new ArrayList<>());
        Mockito.when(assertionRepository.findByEmail(Mockito.eq("1@email.com"))).thenReturn(assertions);
        OrcidRecord recordWithRevokedToken = getOrcidRecordWithRevokedToken();
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.of(getOrcidRecord("1")));

        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenThrow(new DeactivatedException());

        assertionService.postAssertionsToOrcid();

        Mockito.verify(orcidAPIClient).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidRecordService).findOneByEmail(Mockito.eq("1@email.com"));
        Mockito.verify(orcidAPIClient, Mockito.never()).postAffiliation(Mockito.anyString(), Mockito.anyString(), Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository).save(assertionCaptor.capture());
        Assertion assertion = assertionCaptor.getValue();
        assertThat(assertion.getStatus()).isEqualTo(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name());
        Mockito.verify(assertionRepository, Mockito.times(2)).findAllToCreateInOrcidRegistry(pageableCaptor.capture());
        Mockito.verify(orcidRecordService).deleteOrcidRecordByEmail(Mockito.eq("1@email.com"));
    }

    @Test
    void testPostAssertionsToOrcidWithDeprecatedProfile() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeprecatedException, DeactivatedException {
        List<Assertion> assertions = getAssertionsForCreatingInOrcid(1, 1);

        Mockito.when(assertionRepository.findAllToCreateInOrcidRegistry(Mockito.any(Pageable.class)))
            .thenReturn(assertions).thenReturn(new ArrayList<>());
        Mockito.when(assertionRepository.findByEmail(Mockito.eq("1@email.com"))).thenReturn(assertions);
        OrcidRecord recordWithRevokedToken = getOrcidRecordWithRevokedToken();
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.of(getOrcidRecord("1")));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1"), Mockito.anyString())).thenReturn("accessToken");
        Mockito.doThrow(new DeprecatedException()).when(orcidAPIClient).postAffiliation(Mockito.eq("orcid1"), Mockito.eq("accessToken"), Mockito.any(Assertion.class));

        assertionService.postAssertionsToOrcid();

        Mockito.verify(orcidAPIClient).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidRecordService).findOneByEmail(Mockito.eq("1@email.com"));
        Mockito.verify(orcidAPIClient).postAffiliation(Mockito.anyString(), Mockito.anyString(), Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository).save(assertionCaptor.capture());
        Assertion assertion = assertionCaptor.getValue();
        assertThat(assertion.getStatus()).isEqualTo(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name());
        Mockito.verify(assertionRepository, Mockito.times(2)).findAllToCreateInOrcidRegistry(pageableCaptor.capture());
        Mockito.verify(orcidRecordService).deleteOrcidRecordByEmail(Mockito.eq("1@email.com"));
    }

    @Test
    void testPostAssertionToOrcid_statusPendingToInOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.when(orcidAPIClient.postAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"), Mockito.any(Assertion.class))).thenReturn("putCode1234");

        assertionService.postAssertionToOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.IN_ORCID.name(), saved.getStatus());
        assertNull(saved.getUpdatedInORCID());
    }

    @Test
    void testPostAssertionToOrcid_statusPendingToUserRevokedAccess() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.doThrow(new ORCIDAPIException(401, "some message")).when(orcidAPIClient).postAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"),
            Mockito.any(Assertion.class));

        assertionService.postAssertionToOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.USER_REVOKED_ACCESS.name(), saved.getStatus());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.doThrow(new ORCIDAPIException(400, "invalid_scope")).when(orcidAPIClient).postAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"),
            Mockito.any(Assertion.class));

        assertionService.postAssertionToOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(2)).save(assertionCaptor.capture());
        saved = assertionCaptor.getAllValues().get(1);
        assertEquals(AssertionStatus.USER_REVOKED_ACCESS.name(), saved.getStatus());
        assertNull(saved.getUpdatedInORCID());
    }

    @Test
    void testPostAssertionToOrcid_statusPendingToUserDeniedAccess() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        OrcidToken token = new OrcidToken(DEFAULT_SALESFORCE_ID, null);
        token.setDeniedDate(Instant.now());
        orcidRecord.setTokens(Arrays.asList(token));

        Assertion assertion = getAssertionWithEmail("test@orcid.org");

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));

        assertionService.postAssertionToOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.USER_DENIED_ACCESS.name(), saved.getStatus());
    }

    @Test
    void testPostAssertionToOrcid_statusPendingToErrorAddingToOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeprecatedException, DeactivatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.doThrow(new ORCIDAPIException(400, "invalid data")).when(orcidAPIClient).postAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"),
            Mockito.any(Assertion.class));

        assertionService.postAssertionToOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.ERROR_ADDING_TO_ORCID.name(), saved.getStatus());
        assertNull(saved.getUpdatedInORCID());
    }

    @Test
    void testPostAssertionToOrcid_statusPendingRetryToInOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        assertion.setOrcidError("{ statusCode: 400, error: 'something' }");
        Instant addedToOrcidAttempt = Instant.now();
        assertion.setLastSyncAttempt(addedToOrcidAttempt.minusMillis(10000l));
        assertion.setModified(Instant.now());
        assertion.setStatus(AssertionStatus.PENDING_RETRY.name());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.when(orcidAPIClient.postAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"), Mockito.any(Assertion.class))).thenReturn("putCode1234");

        assertionService.postAssertionToOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.IN_ORCID.name(), saved.getStatus());
        assertNull(saved.getUpdatedInORCID());
    }

    @Test
    void testPutAssertionsInOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        Mockito.when(assertionRepository.findAllToUpdateInOrcidRegistry(Mockito.any(Pageable.class)))
            .thenReturn(getAssertionsForUpdateInOrcid(1, AssertionService.REGISTRY_SYNC_BATCH_SIZE))
            .thenReturn(getAssertionsForUpdateInOrcid(AssertionService.REGISTRY_SYNC_BATCH_SIZE + 1, (int) (AssertionService.REGISTRY_SYNC_BATCH_SIZE * 1.5)))
            .thenReturn(new ArrayList<>());

        for (int i = 1; i <= 5; i++) {
            Mockito.when(orcidRecordService.findOneByEmail(i + "@email.com")).thenReturn(Optional.of(getOrcidRecord(Integer.toString(i))));
        }

        for (int i = 6; i <= AssertionService.REGISTRY_SYNC_BATCH_SIZE * 1.5; i++) {
            Mockito.when(orcidRecordService.findOneByEmail(i + "@email.com")).thenReturn(getOptionalOrcidRecord(i));
        }

        for (int i = 1; i <= 5; i++) {
            Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken" + i), Mockito.anyString())).thenReturn("accessToken" + i);
            Mockito.when(orcidAPIClient.postAffiliation(Mockito.eq("orcid" + i), Mockito.eq("accessToken" + i), Mockito.any(Assertion.class))).thenReturn("putCode" + i);
        }

        for (int i = 1; i <= AssertionService.REGISTRY_SYNC_BATCH_SIZE * 1.5; i++) {
            // for when assertion is refreshed
            Mockito.when(assertionRepository.findById(Mockito.eq("id" + i)))
                .thenReturn(Optional.of(getAssertionWithEmailAndPutCode(i + "@email.com", String.valueOf(i))));
        }

        assertionService.putAssertionsInOrcid();

        Mockito.verify(orcidRecordService, Mockito.times((int) (AssertionService.REGISTRY_SYNC_BATCH_SIZE * 1.5))).findOneByEmail(Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.times(5)).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.times(5)).putAffiliation(Mockito.anyString(), Mockito.anyString(), assertionCaptor.capture());

        List<Assertion> posted = assertionCaptor.getAllValues();
        posted.forEach(a -> assertNotNull(a.getLastSyncAttempt()));
    }

    @Test
    void testPutAssertionsInOrcidWithDeactiveProfile() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        List<Assertion> assertions = getAssertionsForUpdateInOrcid(1, 1);
        Mockito.when(assertionRepository.findAllToUpdateInOrcidRegistry(Mockito.any(Pageable.class)))
            .thenReturn(assertions)
            .thenReturn(new ArrayList<>());
        Mockito.when(assertionRepository.findByEmail("1@email.com")).thenReturn(assertions);
        Mockito.when(orcidRecordService.findOneByEmail("1@email.com")).thenReturn(Optional.of(getOrcidRecord("1")));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1"), Mockito.anyString())).thenThrow(new DeactivatedException());

        // for when assertion is refreshed
        Mockito.when(assertionRepository.findById(Mockito.eq("id1")))
            .thenReturn(Optional.of(getAssertionWithEmailAndPutCode("1@email.com", "1")));

        assertionService.putAssertionsInOrcid();

        Mockito.verify(orcidRecordService).findOneByEmail("1@email.com");
        Mockito.verify(orcidAPIClient).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.never()).putAffiliation(Mockito.anyString(), Mockito.anyString(), Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository).save(assertionCaptor.capture());
        Assertion assertion = assertionCaptor.getValue();
        assertThat(assertion.getStatus()).isEqualTo(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name());
        Mockito.verify(orcidRecordService).deleteOrcidRecordByEmail(Mockito.eq("1@email.com"));
    }

    @Test
    void testPutAssertionsInOrcidWithDeprecatedProfile() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        List<Assertion> assertions = getAssertionsForUpdateInOrcid(1, 1);
        Mockito.when(assertionRepository.findAllToUpdateInOrcidRegistry(Mockito.any(Pageable.class)))
            .thenReturn(assertions)
            .thenReturn(new ArrayList<>());
        Mockito.when(assertionRepository.findByEmail("1@email.com")).thenReturn(assertions);
        Mockito.when(orcidRecordService.findOneByEmail("1@email.com")).thenReturn(Optional.of(getOrcidRecord("1")));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1"), Mockito.anyString())).thenReturn("accessToken");
        Mockito.doThrow(new DeprecatedException()).when(orcidAPIClient).putAffiliation(Mockito.eq("orcid1"), Mockito.eq("accessToken"), Mockito.any(Assertion.class));

        // for when assertion is refreshed
        Mockito.when(assertionRepository.findById(Mockito.eq("id1")))
            .thenReturn(Optional.of(getAssertionWithEmailAndPutCode("1@email.com", "1")));

        assertionService.putAssertionsInOrcid();

        Mockito.verify(orcidRecordService).findOneByEmail("1@email.com");
        Mockito.verify(orcidAPIClient).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient).putAffiliation(Mockito.anyString(), Mockito.anyString(), Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository).save(assertionCaptor.capture());
        Assertion assertion = assertionCaptor.getValue();
        assertThat(assertion.getStatus()).isEqualTo(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name());
        Mockito.verify(orcidRecordService).deleteOrcidRecordByEmail(Mockito.eq("1@email.com"));
    }

    @Test
    void testPutAssertionInOrcid_statusPendingRetryToInOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        Instant addedToOrcid = Instant.now();
        assertion.setPutCode("something");
        assertion.setAddedToORCID(addedToOrcid);
        assertion.setLastSyncAttempt(addedToOrcid);
        assertion.setModified(Instant.now());
        assertion.setStatus(AssertionStatus.PENDING_RETRY.name());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");

        assertionService.putAssertionInOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.IN_ORCID.name(), saved.getStatus());
    }

    @Test
    void testPutAssertionInOrcid_statusErrorPendingRetryToInOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        assertion.setOrcidError("{ statusCode: 400, error: 'something' }");
        Instant addedToOrcid = Instant.now();
        assertion.setPutCode("something");
        assertion.setAddedToORCID(addedToOrcid);
        assertion.setLastSyncAttempt(addedToOrcid);
        assertion.setModified(Instant.now());
        assertion.setStatus(AssertionStatus.PENDING_RETRY.name());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");

        assertionService.putAssertionInOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.IN_ORCID.name(), saved.getStatus());
    }

    @Test
    void testPutAssertionInOrcid_statusPendingRetryToUserRevokedAccess() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeprecatedException, DeactivatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        Instant addedToOrcid = Instant.now();
        assertion.setPutCode("something");
        assertion.setAddedToORCID(addedToOrcid);
        assertion.setLastSyncAttempt(addedToOrcid);
        assertion.setModified(Instant.now());
        assertion.setStatus(AssertionStatus.PENDING_RETRY.name());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.doThrow(new ORCIDAPIException(401, "some message")).when(orcidAPIClient).putAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"),
            Mockito.any(Assertion.class));

        assertionService.putAssertionInOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.USER_REVOKED_ACCESS.name(), saved.getStatus());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.doThrow(new ORCIDAPIException(400, "invalid_scope")).when(orcidAPIClient).putAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"),
            Mockito.any(Assertion.class));

        assertionService.putAssertionInOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(2)).save(assertionCaptor.capture());
        saved = assertionCaptor.getAllValues().get(1);
        assertEquals(AssertionStatus.USER_REVOKED_ACCESS.name(), saved.getStatus());
    }

    @Test
    void testPutAssertionInOrcid_statusPendingToUserDeniedAccess() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        OrcidToken token = new OrcidToken(DEFAULT_SALESFORCE_ID, null);
        token.setDeniedDate(Instant.now());
        orcidRecord.setTokens(Arrays.asList(token));

        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        assertion.setStatus(AssertionStatus.PENDING_RETRY.name());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));

        assertionService.putAssertionInOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.USER_DENIED_ACCESS.name(), saved.getStatus());
    }

    @Test
    void testPutAssertionInOrcid_statusPendingRetryToErrorUpdatingInOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException, DeactivatedException, DeprecatedException {
        OrcidRecord orcidRecord = getOrcidRecord("1234");
        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        Instant addedToOrcid = Instant.now();
        assertion.setPutCode("something");
        assertion.setAddedToORCID(addedToOrcid);
        assertion.setLastSyncAttempt(addedToOrcid);
        assertion.setModified(Instant.now());
        assertion.setStatus(AssertionStatus.PENDING_RETRY.name());

        Mockito.when(orcidRecordService.findOneByEmail("test@orcid.org")).thenReturn(Optional.of(orcidRecord));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken1234"), Mockito.anyString())).thenReturn("accessToken1234");
        Mockito.doThrow(new ORCIDAPIException(400, "invalid data")).when(orcidAPIClient).putAffiliation(Mockito.eq("orcid1234"), Mockito.eq("accessToken1234"),
            Mockito.any(Assertion.class));

        assertionService.putAssertionInOrcid(assertion);

        Mockito.verify(assertionRepository, Mockito.times(1)).save(assertionCaptor.capture());
        Assertion saved = assertionCaptor.getValue();
        assertEquals(AssertionStatus.ERROR_UPDATING_TO_ORCID.name(), saved.getStatus());
    }

// @Test
// void testDeleteAssertionFromOrcidRegistry_successfulDelete() throws
// org.json.JSONException, ClientProtocolException, IOException {
// Mockito.when(assertionsRepository.findById(Mockito.eq("assertionId"))).thenReturn(Optional.of(getAssertionWithEmail("something@orcid.org")));
// Mockito.when(orcidRecordService.findOneByEmail("something@orcid.org")).thenReturn(getOptionalOrcidRecordWithIdToken());
// Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString())).thenReturn("accessToken");
// Mockito.when(orcidAPIClient.deleteAffiliation(Mockito.anyString(),
// Mockito.eq("accessToken"),
// Mockito.any(Assertion.class))).thenReturn(true);
// assertionService.deleteAssertionFromOrcidRegistry("assertionId",
// getUser());
//
// Mockito.verify(orcidAPIClient,
// Mockito.times(1)).exchangeToken(Mockito.anyString());
// Mockito.verify(orcidAPIClient,
// Mockito.times(1)).deleteAffiliation(Mockito.anyString(),
// Mockito.anyString(), Mockito.any(Assertion.class));
//
// Mockito.verify(assertionsRepository,
// Mockito.times(1)).save(assertionCaptor.capture());
// List<Assertion> captured = assertionCaptor.getAllValues();
// Assertion lastSaved = captured.get(captured.size() - 1);
// assertNotNull(lastSaved.getLastSyncAttempt());
// }
//
// @Test
// void testDeleteAssertionFromOrcidRegistry_failedDelete() throws
// org.json.JSONException, ClientProtocolException, IOException {
// Assertion assertion = getAssertionWithEmail("something@orcid.org");
// Mockito.when(assertionsRepository.findById(Mockito.eq("assertionId"))).thenReturn(Optional.of(assertion));
// Mockito.when(orcidRecordService.findOneByEmail("something@orcid.org")).thenReturn(getOptionalOrcidRecordWithIdToken());
// Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString())).thenReturn("accessToken");
// Mockito.when(orcidAPIClient.deleteAffiliation(Mockito.anyString(),
// Mockito.eq("accessToken"),
// Mockito.any(Assertion.class))).thenReturn(false);
// assertionService.deleteAssertionFromOrcidRegistry("assertionId",
// getUser());
//
// Mockito.verify(orcidAPIClient,
// Mockito.times(1)).exchangeToken(Mockito.anyString());
// Mockito.verify(orcidAPIClient,
// Mockito.times(1)).deleteAffiliation(Mockito.anyString(),
// Mockito.anyString(), Mockito.any(Assertion.class));
//
// assertNotNull(assertion.getLastSyncAttempt());
// }
//
// @Test
// void testDeleteAssertionFromOrcidRegistry_errorDeleting() throws
// org.json.JSONException, ClientProtocolException, IOException {
// Assertion assertion = getAssertionWithEmail("something@orcid.org");
// assertion.setId("assertionId");
// Mockito.when(assertionsRepository.findById(Mockito.eq("assertionId"))).thenReturn(Optional.of(assertion));
// Mockito.when(assertionsRepository.save(Mockito.any(Assertion.class))).thenReturn(assertion);
// Mockito.when(orcidRecordService.findOneByEmail("something@orcid.org")).thenReturn(getOptionalOrcidRecordWithIdToken());
// Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString())).thenReturn("accessToken");
// Mockito.doThrow(new ORCIDAPIException(404, "not
// found")).when(orcidAPIClient).deleteAffiliation(Mockito.anyString(),
// Mockito.eq("accessToken"),
// Mockito.any(Assertion.class));
// assertionService.deleteAssertionFromOrcidRegistry("assertionId",
// getUser());
//
// Mockito.verify(orcidAPIClient,
// Mockito.times(1)).exchangeToken(Mockito.anyString());
// Mockito.verify(orcidAPIClient,
// Mockito.times(1)).deleteAffiliation(Mockito.anyString(),
// Mockito.anyString(), Mockito.any(Assertion.class));
//
// Mockito.verify(assertionsRepository,
// Mockito.times(1)).save(assertionCaptor.capture());
// List<Assertion> captured = assertionCaptor.getAllValues();
// Assertion lastSaved = captured.get(captured.size() - 1);
// assertNotNull(lastSaved.getLastSyncAttempt());
// }

    @Test
    void testFindByEmail() {
        String email = "email@email.com";
        Assertion assertion = getAssertionWithEmail(email);
        Mockito.when(assertionRepository.findByEmail(Mockito.eq(email))).thenReturn(Arrays.asList(assertion));

        List<Assertion> assertions = assertionService.findByEmail(email);
        assertFalse(assertions.isEmpty());
        assertEquals(1, assertions.size());
        assertEquals(AssertionStatus.PENDING.getValue(), assertions.get(0).getPrettyStatus());
        Mockito.verify(assertionRepository, Mockito.times(1)).findByEmail(Mockito.eq(email));
    }

    @Test
    void testFindById() {
        Assertion a = getAssertionWithEmail("something@orcid.org");
        Mockito.when(assertionRepository.findById(Mockito.anyString())).thenReturn(Optional.of(a));
        Mockito.when(orcidRecordService.generateLinkForEmail(Mockito.eq("something@orcid.org"))).thenReturn("permission-link");

        Assertion assertion = assertionService.findById("id");
        assertNotNull(assertion);
        assertNull(assertion.getPermissionLink());
        assertEquals(AssertionStatus.PENDING.getValue(), assertion.getPrettyStatus());
    }

    @Test
    void testGenerateAssertionsCSV() throws IOException {
        Mockito.when(assertionsUserService.getLoggedInUserId()).thenReturn("test");
        Mockito.doNothing().when(csvReportService).storeCsvReportRequest(Mockito.eq("test"), Mockito.anyString(), Mockito.eq(CsvReport.ASSERTIONS_FOR_EDIT_TYPE));
        assertionService.generateAssertionsCSV();
        Mockito.verify(csvReportService).storeCsvReportRequest(Mockito.eq("test"), Mockito.anyString(), Mockito.eq(CsvReport.ASSERTIONS_FOR_EDIT_TYPE));
    }

    @Test
    void testGenerateAssertionsReport() throws IOException {
        Mockito.when(assertionsUserService.getLoggedInUserId()).thenReturn("test");
        Mockito.doNothing().when(csvReportService).storeCsvReportRequest(Mockito.eq("test"), Mockito.anyString(), Mockito.eq(CsvReport.ASSERTIONS_REPORT_TYPE));
        assertionService.generateAssertionsReport();
        Mockito.verify(csvReportService).storeCsvReportRequest(Mockito.eq("test"), Mockito.anyString(), Mockito.eq(CsvReport.ASSERTIONS_REPORT_TYPE));
    }

    @Test
    void testGeneratePermissionLinks() throws IOException {
        Mockito.when(assertionsUserService.getLoggedInUserId()).thenReturn("test");
        Mockito.doNothing().when(csvReportService).storeCsvReportRequest(Mockito.eq("test"), Mockito.anyString(), Mockito.eq(CsvReport.PERMISSION_LINKS_TYPE));
        assertionService.generatePermissionLinks();
        Mockito.verify(csvReportService).storeCsvReportRequest(Mockito.eq("test"), Mockito.anyString(), Mockito.eq(CsvReport.PERMISSION_LINKS_TYPE));
    }

    @Test
    void testDeleteByIdWithRegistryDeleteAndOtherAssertionsForUser() throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmailAndPutCode("test@orcid.org", "1001");
        assertion.setSalesforceId("salesforce-id");

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenReturn("exchange-token");
        Mockito.when(assertionRepository.countByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(2l);

        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        assertionService.deleteById("id", getUser());

        Mockito.verify(assertionRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.never()).deleteOrcidRecord(Mockito.any());

        Mockito.verify(assertionRepository, Mockito.times(1)).findById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.atLeastOnce()).findOneByEmail(Mockito.eq("test@orcid.org"));
        Mockito.verify(orcidAPIClient, Mockito.times(1)).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.times(1)).deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"), Mockito.any(Assertion.class));

    }

    @Test
    void testDeleteByIdAlreadyDeletedInTheRegistry() throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmailAndPutCode("test@orcid.org", "1001");
        assertion.setSalesforceId("salesforce-id");

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenReturn("exchange-token");
        Mockito.doThrow(new ORCIDAPIException(404, "already deleted")).when(orcidAPIClient).deleteAffiliation(Mockito.eq("orcid"), Mockito.eq("exchange-token"),
            Mockito.any(Assertion.class));
        Mockito.when(assertionRepository.countByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(2l);

        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        assertionService.deleteById("id", getUser());

        Mockito.verify(assertionRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.never()).deleteOrcidRecord(Mockito.any());

        Mockito.verify(assertionRepository, Mockito.times(1)).findById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.atLeastOnce()).findOneByEmail(Mockito.eq("test@orcid.org"));
        Mockito.verify(orcidAPIClient, Mockito.times(1)).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.times(1)).deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
    }

    @Test
    void testDeleteByIdDeprecatedOrDeactivated() throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmailAndPutCode("test@orcid.org", "1001");
        assertion.setSalesforceId("salesforce-id");
        assertion.setStatus(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name());

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(Optional.empty());
        Mockito.when(assertionRepository.countByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(2l);
        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        assertionService.deleteById("id", getUser());

        Mockito.verify(assertionRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.never()).deleteOrcidRecord(Mockito.any());
        Mockito.verify(assertionRepository, Mockito.times(1)).findById(Mockito.eq("id"));
        Mockito.verify(orcidAPIClient, Mockito.never()).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.never()).deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
    }

    @Test
    void testDeleteByIdWithRegistryDeleteFailure() throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmailAndPutCode("test@orcid.org", "1001");
        assertion.setSalesforceId("salesforce-id");

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenReturn("exchange-token");
        Mockito.doThrow(new ORCIDAPIException(500, "something bad")).when(orcidAPIClient).deleteAffiliation(Mockito.eq("orcid"), Mockito.eq("exchange-token"),
            Mockito.any(Assertion.class));

        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        Assertions.assertThrows(RegistryDeleteFailureException.class, () -> {
            assertionService.deleteById("id", getUser());
        });

        Mockito.verify(assertionRepository, Mockito.never()).deleteById(Mockito.eq("id"));
        Mockito.verify(assertionRepository).save(assertionCaptor.capture());
        Assertion updated = assertionCaptor.getValue();
        assertNotNull(updated.getOrcidError());
        assertEquals(AssertionStatus.ERROR_DELETING_IN_ORCID.name(), updated.getStatus());
    }

    @Test
    void testDeleteByIdWithRevokedToken() throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmailAndPutCode("test@orcid.org", "1001");
        assertion.setSalesforceId("salesforce-id");

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(Optional.of(getOrcidRecordWithRevokedToken()));
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenReturn("exchange-token");
        Mockito.when(assertionRepository.countByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(0l);

        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        assertionService.deleteById("id", getUser());

        Mockito.verify(assertionRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService).deleteOrcidRecordTokenByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID));

        Mockito.verify(assertionRepository, Mockito.times(1)).findById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.atLeastOnce()).findOneByEmail(Mockito.eq("test@orcid.org"));
        Mockito.verify(orcidAPIClient, Mockito.times(1)).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.times(1)).deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
    }

    @Test
    void testDeleteByIdWithRegistryDeleteAndNoOtherAssertionsForUser()
        throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmailAndPutCode("test@orcid.org", "1001");
        assertion.setSalesforceId("salesforce-id");

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenReturn("exchange-token");
        Mockito.when(assertionRepository.countByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(0l);

        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        assertionService.deleteById("id", getUser());

        Mockito.verify(assertionRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService).deleteOrcidRecordTokenByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID));

        Mockito.verify(assertionRepository, Mockito.times(1)).findById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.atLeastOnce()).findOneByEmail(Mockito.eq("test@orcid.org"));
        Mockito.verify(orcidAPIClient, Mockito.times(1)).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.times(1)).deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
    }

    @Test
    void testDeleteByIdWithoutRegistryDeleteAndOtherAssertionsForUser()
        throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        assertion.setSalesforceId("salesforce-id");

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenReturn("exchange-token");
        Mockito.when(assertionRepository.countByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(2l);

        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        assertionService.deleteById("id", getUser());

        Mockito.verify(assertionRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService, Mockito.never()).deleteOrcidRecord(Mockito.any());
        Mockito.verify(orcidAPIClient, Mockito.never()).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.never()).deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
    }

    @Test
    void testDeleteByIdWithoutRegistryDeleteAndNoOtherAssertionsForUser()
        throws org.json.JSONException, ClientProtocolException, IOException, RegistryDeleteFailureException, DeactivatedException, DeprecatedException {
        Assertion assertion = getAssertionWithEmail("test@orcid.org");
        assertion.setSalesforceId("salesforce-id");

        Mockito.when(assertionRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithIdToken());
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString(), Mockito.anyString())).thenReturn("exchange-token");
        Mockito.when(assertionRepository.countByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(0l);

        Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.eq("id"));

        assertionService.deleteById("id", getUser());

        Mockito.verify(assertionRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
        Mockito.verify(orcidRecordService).deleteOrcidRecordTokenByEmailAndSalesforceId(Mockito.eq("test@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID));
        Mockito.verify(orcidAPIClient, Mockito.never()).exchangeToken(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(orcidAPIClient, Mockito.never()).deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
    }

    @Test
    void testDeleteAllBySalesforceId_orcidRecordsDeleted() {
        Mockito.when(assertionRepository.findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Sort.class))).thenReturn(getAssertionsForUpdateInOrcid(1, 20));
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.anyString());
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.empty());

        assertionService.deleteAllBySalesforceId("salesforce-id");

        Mockito.verify(assertionRepository, Mockito.times(1)).findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Sort.class));
        Mockito.verify(assertionRepository, Mockito.times(20)).deleteById(Mockito.anyString());
        Mockito.verify(orcidRecordService, Mockito.never()).deleteOrcidRecord(Mockito.any(OrcidRecord.class));
    }

    @Test
    void testDeleteAllBySalesforceId_orcidRecordsNotDeleted() {
        Mockito.when(assertionRepository.findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Sort.class))).thenReturn(getAssertionsForUpdateInOrcid(1, 20));
        Mockito.doNothing().when(assertionRepository).deleteById(Mockito.anyString());
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getOptionalOrcidRecordWithoutIdToken());

        assertionService.deleteAllBySalesforceId("salesforce-id");

        Mockito.verify(assertionRepository, Mockito.times(1)).findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Sort.class));
        Mockito.verify(assertionRepository, Mockito.times(20)).deleteById(Mockito.anyString());
        Mockito.verify(orcidRecordService, Mockito.times(20)).deleteOrcidRecordByEmail(Mockito.anyString());
    }

    @Test
    void testFindBySalesforceId() {
        Mockito.when(assertionRepository.findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Pageable.class)))
            .thenReturn(new PageImpl<Assertion>(Arrays.asList(getAssertionWithEmail("email@orcid.org"), getAssertionWithEmail("email@orcid.org"))));
        Mockito.when(assertionRepository
                .findBySalesforceIdAndAffiliationSectionContainingIgnoreCaseOrSalesforceIdAndDepartmentNameContainingIgnoreCaseOrSalesforceIdAndOrgNameContainingIgnoreCaseOrSalesforceIdAndDisambiguatedOrgIdContainingIgnoreCaseOrSalesforceIdAndEmailContainingIgnoreCaseOrSalesforceIdAndOrcidIdContainingIgnoreCaseOrSalesforceIdAndRoleTitleContainingIgnoreCase(
                    Mockito.any(Pageable.class), Mockito.eq("salesforce-id"), Mockito.eq("filter"), Mockito.eq("salesforce-id"), Mockito.eq("filter"),
                    Mockito.eq("salesforce-id"), Mockito.eq("filter"), Mockito.eq("salesforce-id"), Mockito.eq("filter"), Mockito.eq("salesforce-id"),
                    Mockito.eq("filter"), Mockito.eq("salesforce-id"), Mockito.eq("filter"), Mockito.eq("salesforce-id"), Mockito.eq("filter")))
            .thenReturn(new PageImpl<Assertion>(Arrays.asList(getAssertionWithEmail("email@orcid.org"))));

        Page<Assertion> page = assertionService.findByCurrentSalesforceId(Mockito.mock(Pageable.class));
        assertEquals(2, page.getTotalElements());
        assertEquals(AssertionStatus.PENDING.getValue(), page.getContent().get(0).getPrettyStatus());
        assertEquals(AssertionStatus.PENDING.getValue(), page.getContent().get(1).getPrettyStatus());
        page = assertionService.findBySalesforceId(Mockito.mock(Pageable.class), "filter");
        assertEquals(1, page.getTotalElements());
        assertEquals(AssertionStatus.PENDING.getValue(), page.getContent().get(0).getPrettyStatus());
    }

//
// @Test
// void testDeleteAssertionFromOrcidRegistry_wrongSalesforceId() throws
// org.json.JSONException, ClientProtocolException, IOException,
// JAXBException {
// Assertion assertion = getAssertionWithEmailAndPutCode("test@email.com",
// "1001");
// assertion.setSalesforceId("salesforce-id");
// Mockito.when(assertionsRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
// Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("wrong-salesforce-id");
//
// AssertionServiceUser user = getUser();
// user.setSalesforceId("something-wrong");
// Assertions.assertThrows(BadRequestAlertException.class, () -> {
// assertionService.deleteAssertionFromOrcidRegistry("id", user);
// });
// }
//
// @Test
// void testDeleteAssertionFromOrcidRegistry_CannotDelete() throws
// org.json.JSONException, ClientProtocolException, IOException,
// JAXBException {
// Assertion assertion = getAssertionWithEmailAndPutCode("test@email.com",
// "1001");
// assertion.setSalesforceId("salesforce-id");
//
// Mockito.when(assertionsRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
// Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
// Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@email.com"))).thenReturn(getOptionalOrcidRecordWithoutIdToken());
//
// assertionService.deleteAssertionFromOrcidRegistry("id", getUser());
//
// Mockito.verify(assertionsRepository,
// Mockito.times(1)).findById(Mockito.eq("id"));
// Mockito.verify(orcidRecordService,
// Mockito.atLeastOnce()).findOneByEmail(Mockito.eq("test@email.com"));
// Mockito.verify(orcidAPIClient,
// Mockito.never()).exchangeToken(Mockito.anyString());
// Mockito.verify(orcidAPIClient,
// Mockito.never()).deleteAffiliation(Mockito.anyString(),
// Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
// }

    @Test
    void testIsDuplicate() {
        Assertion a = getAssertionWithoutIdForEmail("email");
        Assertion b = getAssertionWithoutIdForEmail("email");
        b.setRoleTitle("something different");
        Assertion c = getAssertionWithoutIdForEmail("email");
        c.setEndDay("09");
        Assertion d = getAssertionWithoutIdForEmail("email");
        d.setAffiliationSection(AffiliationSection.EMPLOYMENT);

        Assertion comparison = getAssertionWithoutIdForEmail("email"); // duplicate
        // of
        // assertion
        // a
        Mockito.when(assertionRepository.findByEmailAndSalesforceId(Mockito.eq("email"), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(Arrays.asList(b, c, a, d));
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setUrl("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));


        a.setUrl(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setUrl("url");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        comparison.setUrl(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        comparison.setUrl("url");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setUrl(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setUrl("url");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        comparison.setUrl("");
        a.setUrl(null);
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        comparison.setUrl("url");
        a.setUrl("url");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setStartMonth("08");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setStartMonth(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setStartMonth("01");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        comparison.setStartMonth(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        comparison.setStartMonth("01");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setStartYear("1981");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setStartYear(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setStartYear("2020");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndDay("02");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndDay(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndDay("01");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndMonth("06");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndMonth(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndMonth("01");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndYear("1981");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndYear(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setEndYear("2021");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setOrgName("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setOrgName(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setOrgName("org");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setOrgCity("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setOrgCity(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setOrgCity("city");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setDisambiguationSource("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setDisambiguationSource(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setDisambiguationSource("RINGGOLD");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setDisambiguatedOrgId("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setDisambiguatedOrgId(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setDisambiguatedOrgId("id");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalId("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalId(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalId("extId");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalIdType("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalIdType(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalIdType("extIdType");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalIdUrl("something-different");
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalIdUrl(null);
        assertFalse(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        a.setExternalIdUrl("extIdUrl");
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));

        comparison.setId("not-null"); // id should be ignored
        assertTrue(assertionService.isDuplicate(comparison, DEFAULT_SALESFORCE_ID));
        verify(assertionNormalizer, Mockito.times(49)).normalize(Mockito.any(Assertion.class)); // check normalization was used
    }

    @Test
    void testProcessAssertionUploadsNoProcessingIfErrorsPresent() throws IOException {
        Mockito.when(storedFileService.getUnprocessedStoredFilesByType(Mockito.eq(StoredFileService.ASSERTIONS_CSV_FILE_TYPE)))
            .thenReturn(Arrays.asList(getDummyStoredFile()));
        Mockito.when(assertionsUserService.getUserById(Mockito.eq("owner"))).thenReturn(getUser());

        AssertionsUpload upload = new AssertionsUpload();
        upload.addAssertion(getAssertionWithEmail("1@email.com"));
        upload.addAssertion(getAssertionWithEmail("2@email.com"));
        upload.addAssertion(getAssertionWithEmail("3@email.com"));
        upload.addError(1, "test error");

        Mockito.when(assertionsCsvReader.readAssertionsUpload(Mockito.any(InputStream.class), Mockito.any(AssertionServiceUser.class))).thenReturn(upload);

        assertionService.processAssertionUploads();

        Mockito.verify(mailService).sendAssertionsUploadSummaryMail(Mockito.any(AssertionsUploadSummary.class), Mockito.any(AssertionServiceUser.class));
        Mockito.verify(assertionRepository, Mockito.never()).insert(Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository, Mockito.never()).save(Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository, Mockito.never()).delete(Mockito.any(Assertion.class));
    }

    @Test
    void testUploadAssertions() throws IOException {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.when(file.getOriginalFilename()).thenReturn("some-file.csv");
        Mockito.when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        assertionService.uploadAssertions(file);
        Mockito.verify(storedFileService).storeAssertionsCsvFile(Mockito.any(InputStream.class), filenameCaptor.capture(), Mockito.any(AssertionServiceUser.class));

        String filename = filenameCaptor.getValue();
        assertEquals("some-file.csv", filename);
    }

    @Test
    void testProcessAssertionUploads() throws IOException, DeprecatedException, DeactivatedException {
        Mockito.when(storedFileService.getUnprocessedStoredFilesByType(Mockito.eq(StoredFileService.ASSERTIONS_CSV_FILE_TYPE)))
            .thenReturn(Arrays.asList(getDummyStoredFile()));
        Mockito.when(assertionsUserService.getUserById(Mockito.eq("owner"))).thenReturn(getUser());

        AssertionsUpload upload = new AssertionsUpload();
        upload.addAssertion(getAssertionWithEmail("1@email.com"));
        upload.addAssertion(getAssertionWithEmail("2@email.com"));
        upload.addAssertion(getAssertionWithEmail("3@email.com"));

        Assertion toUpdate = getAssertionWithEmail("4@email.com");
        toUpdate.setId("12346");
        upload.addAssertion(toUpdate);

        Assertion deleteOne = new Assertion();
        deleteOne.setId("9999");
        Assertion deleteTwo = new Assertion();
        deleteTwo.setId("6666");
        Assertion deleteThree = new Assertion();
        deleteThree.setId("7777");

        upload.addAssertion(deleteOne);
        upload.addAssertion(deleteTwo);
        upload.addAssertion(deleteThree);

        Assertion deleteOneFull = new Assertion();
        deleteOneFull.setId("9999");
        deleteOneFull.setEmail("9999@email.com");
        deleteOneFull.setSalesforceId(DEFAULT_SALESFORCE_ID);
        deleteOneFull.setPutCode("9999");
        Assertion deleteTwoFull = new Assertion();
        deleteTwoFull.setId("6666");
        deleteTwoFull.setEmail("6666@email.com");
        deleteTwoFull.setSalesforceId(DEFAULT_SALESFORCE_ID);
        deleteTwoFull.setPutCode("6666");
        Assertion deleteThreeFull = new Assertion();
        deleteThreeFull.setId("7777");
        deleteThreeFull.setEmail("7777@email.com");
        deleteThreeFull.setSalesforceId(DEFAULT_SALESFORCE_ID);
        deleteThreeFull.setPutCode("7777");

        OrcidRecord deleteOneRecord = new OrcidRecord();
        deleteOneRecord.setOrcid("9999");
        deleteOneRecord.setEmail("9999@email.com");
        deleteOneRecord.setTokens(Arrays.asList(new OrcidToken(DEFAULT_SALESFORCE_ID, "token")));

        OrcidRecord deleteTwoRecord = new OrcidRecord();
        deleteTwoRecord.setOrcid("6666");
        deleteTwoRecord.setEmail("6666@email.com");
        deleteTwoRecord.setTokens(Arrays.asList(new OrcidToken(DEFAULT_SALESFORCE_ID, "token")));

        OrcidRecord deleteThreeRecord = new OrcidRecord();
        deleteThreeRecord.setOrcid("7777");
        deleteThreeRecord.setEmail("7777@email.com");
        deleteThreeRecord.setTokens(Arrays.asList(new OrcidToken(DEFAULT_SALESFORCE_ID, "token")));

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("1@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("2@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("3@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("4@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("6666@email.com"))).thenReturn(Optional.of(deleteTwoRecord));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("7777@email.com"))).thenReturn(Optional.of(deleteThreeRecord));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("9999@email.com"))).thenReturn(Optional.of(deleteOneRecord));
        Mockito.when(assertionRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                return assertion;
            }
        });
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12346");
                return assertion;
            }
        });
        Mockito.when(assertionRepository.findById(Mockito.eq("12346"))).thenReturn(Optional.of(toUpdate));
        Mockito.when(assertionRepository.findById(Mockito.eq("9999"))).thenReturn(Optional.of(deleteOneFull));
        Mockito.when(assertionRepository.findById(Mockito.eq("6666"))).thenReturn(Optional.of(deleteTwoFull));
        Mockito.when(assertionRepository.findById(Mockito.eq("7777"))).thenReturn(Optional.of(deleteThreeFull));
        Mockito.when(assertionsCsvReader.readAssertionsUpload(Mockito.any(InputStream.class), Mockito.any(AssertionServiceUser.class))).thenReturn(upload);

        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("token"), Mockito.anyString())).thenReturn("token");
        Mockito.doNothing().when(orcidAPIClient).deleteAffiliation(Mockito.eq("6666"), Mockito.eq("token"), Mockito.any(Assertion.class));
        Mockito.doNothing().when(orcidAPIClient).deleteAffiliation(Mockito.eq("7777"), Mockito.eq("token"), Mockito.any(Assertion.class));
        Mockito.doNothing().when(orcidAPIClient).deleteAffiliation(Mockito.eq("9999"), Mockito.eq("token"), Mockito.any(Assertion.class));

        assertionService.processAssertionUploads();

        Mockito.verify(mailService).sendAssertionsUploadSummaryMail(summaryCaptor.capture(), Mockito.any(AssertionServiceUser.class));
        AssertionsUploadSummary summary = summaryCaptor.getValue();
        assertEquals(3, summary.getNumAdded());
        assertEquals(0, summary.getNumDuplicates());
        assertEquals(3, summary.getNumDeleted());
        assertEquals(1, summary.getNumUpdated());
        assertEquals("original-filename.csv", summary.getFilename());
        assertNotNull(summary.getDate());

        Mockito.verify(assertionRepository, Mockito.times(3)).insert(Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository, Mockito.times(1)).save(Mockito.any(Assertion.class));
    }

    @Test
    void testProcessAssertionUploadsWithDeleteFailures() throws IOException, DeprecatedException, DeactivatedException {
        Mockito.when(storedFileService.getUnprocessedStoredFilesByType(Mockito.eq(StoredFileService.ASSERTIONS_CSV_FILE_TYPE)))
            .thenReturn(Arrays.asList(getDummyStoredFile()));
        Mockito.when(assertionsUserService.getUserById(Mockito.eq("owner"))).thenReturn(getUser());

        AssertionsUpload upload = new AssertionsUpload();
        upload.addAssertion(getAssertionWithEmail("1@email.com"));
        upload.addAssertion(getAssertionWithEmail("2@email.com"));
        upload.addAssertion(getAssertionWithEmail("3@email.com"));

        Assertion toUpdate = getAssertionWithEmail("4@email.com");
        toUpdate.setId("12346");
        upload.addAssertion(toUpdate);

        Assertion deleteOne = new Assertion();
        deleteOne.setId("9999");
        Assertion deleteTwo = new Assertion();
        deleteTwo.setId("6666");
        Assertion deleteThree = new Assertion();
        deleteThree.setId("7777");

        upload.addAssertion(deleteOne);
        upload.addAssertion(deleteTwo);
        upload.addAssertion(deleteThree);

        Assertion deleteOneFull = new Assertion();
        deleteOneFull.setId("9999");
        deleteOneFull.setEmail("9999@email.com");
        deleteOneFull.setSalesforceId(DEFAULT_SALESFORCE_ID);
        deleteOneFull.setPutCode("9999");
        Assertion deleteTwoFull = new Assertion();
        deleteTwoFull.setId("6666");
        deleteTwoFull.setEmail("6666@email.com");
        deleteTwoFull.setSalesforceId(DEFAULT_SALESFORCE_ID);
        deleteTwoFull.setPutCode("6666");
        Assertion deleteThreeFull = new Assertion();
        deleteThreeFull.setId("7777");
        deleteThreeFull.setEmail("7777@email.com");
        deleteThreeFull.setSalesforceId(DEFAULT_SALESFORCE_ID);
        deleteThreeFull.setPutCode("7777");

        OrcidRecord deleteOneRecord = new OrcidRecord();
        deleteOneRecord.setOrcid("9999");
        deleteOneRecord.setEmail("9999@email.com");
        deleteOneRecord.setTokens(Arrays.asList(new OrcidToken(DEFAULT_SALESFORCE_ID, "token")));

        OrcidRecord deleteTwoRecord = new OrcidRecord();
        deleteTwoRecord.setOrcid("6666");
        deleteTwoRecord.setEmail("6666@email.com");
        deleteTwoRecord.setTokens(Arrays.asList(new OrcidToken(DEFAULT_SALESFORCE_ID, "token")));

        OrcidRecord deleteThreeRecord = new OrcidRecord();
        deleteThreeRecord.setOrcid("7777");
        deleteThreeRecord.setEmail("7777@email.com");
        deleteThreeRecord.setTokens(Arrays.asList(new OrcidToken(DEFAULT_SALESFORCE_ID, "token")));

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("1@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("2@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("3@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("4@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("6666@email.com"))).thenReturn(Optional.of(deleteTwoRecord));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("7777@email.com"))).thenReturn(Optional.of(deleteThreeRecord));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("9999@email.com"))).thenReturn(Optional.of(deleteOneRecord));
        Mockito.when(assertionRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                return assertion;
            }
        });
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12346");
                return assertion;
            }
        });
        Mockito.when(assertionRepository.findById(Mockito.eq("12346"))).thenReturn(Optional.of(toUpdate));
        Mockito.when(assertionRepository.findById(Mockito.eq("9999"))).thenReturn(Optional.of(deleteOneFull));
        Mockito.when(assertionRepository.findById(Mockito.eq("6666"))).thenReturn(Optional.of(deleteTwoFull));
        Mockito.when(assertionRepository.findById(Mockito.eq("7777"))).thenReturn(Optional.of(deleteThreeFull));
        Mockito.when(assertionsCsvReader.readAssertionsUpload(Mockito.any(InputStream.class), Mockito.any(AssertionServiceUser.class))).thenReturn(upload);
        Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("token"), Mockito.anyString())).thenReturn("token");

        Mockito.doThrow(new ORCIDAPIException(500, "some registry problem")).when(orcidAPIClient).deleteAffiliation(Mockito.eq("6666"), Mockito.eq("token"),
            Mockito.any(Assertion.class));
        Mockito.doNothing().when(orcidAPIClient).deleteAffiliation(Mockito.eq("7777"), Mockito.eq("token"), Mockito.any(Assertion.class));
        Mockito.doNothing().when(orcidAPIClient).deleteAffiliation(Mockito.eq("9999"), Mockito.eq("token"), Mockito.any(Assertion.class));

        assertionService.processAssertionUploads();

        Mockito.verify(mailService).sendAssertionsUploadSummaryMail(summaryCaptor.capture(), Mockito.any(AssertionServiceUser.class));
        AssertionsUploadSummary summary = summaryCaptor.getValue();
        assertEquals(3, summary.getNumAdded());
        assertEquals(0, summary.getNumDuplicates());
        assertEquals(2, summary.getNumDeleted());
        assertEquals(1, summary.getNumUpdated());
        assertEquals(1, summary.getRegistryDeleteFailures().size());
        assertEquals("6666", summary.getRegistryDeleteFailures().get(0));
        assertEquals("original-filename.csv", summary.getFilename());
        assertNotNull(summary.getDate());

        Mockito.verify(assertionRepository, Mockito.times(3)).insert(Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository, Mockito.times(2)).save(Mockito.any(Assertion.class));
    }

    @Test
    void testProcessAssertionUploadsWhereErrorOccursInOneUpload() throws IOException {
        Mockito.when(storedFileService.getUnprocessedStoredFilesByType(Mockito.eq(StoredFileService.ASSERTIONS_CSV_FILE_TYPE))).thenReturn(getDummyStoredFiles());
        Mockito.when(assertionsUserService.getUserById(Mockito.eq("owner"))).thenReturn(getUser());

        AssertionsUpload upload = new AssertionsUpload();
        upload.addAssertion(getAssertionWithEmail("1@email.com"));
        upload.addAssertion(getAssertionWithEmail("2@email.com"));
        upload.addAssertion(getAssertionWithEmail("3@email.com"));

        Assertion toUpdate = getAssertionWithEmail("4@email.com");
        toUpdate.setId("12346");
        upload.addAssertion(toUpdate);

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("1@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("2@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("3@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("4@email.com"))).thenReturn(Optional.of(new OrcidRecord()));
        Mockito.when(assertionRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12345");
                return assertion;
            }
        });
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("12346");
                return assertion;
            }
        });
        Mockito.when(assertionRepository.findById(Mockito.eq("12346"))).thenReturn(Optional.of(toUpdate));
        Mockito.when(assertionsCsvReader.readAssertionsUpload(Mockito.any(InputStream.class), Mockito.any(AssertionServiceUser.class)))
            .thenThrow(new IOException("testing error message")).thenReturn(upload);

        assertionService.processAssertionUploads();

        Mockito.verify(mailService).sendAssertionsUploadSummaryMail(summaryCaptor.capture(), Mockito.any(AssertionServiceUser.class));
        AssertionsUploadSummary summary = summaryCaptor.getValue();
        assertEquals(3, summary.getNumAdded());
        assertEquals(0, summary.getNumDuplicates());
        assertEquals(0, summary.getNumDeleted());
        assertEquals(1, summary.getNumUpdated());

        Mockito.verify(assertionRepository, Mockito.times(3)).insert(Mockito.any(Assertion.class));
        Mockito.verify(assertionRepository, Mockito.times(1)).save(Mockito.any(Assertion.class));
        Mockito.verify(storedFileService, Mockito.times(2)).markAsProcessed(storedFileCaptor.capture());

        List<StoredFile> storedFiles = storedFileCaptor.getAllValues();
        assertEquals(2, storedFiles.size());
        assertNotNull(storedFiles.get(0).getError());
        assertEquals("java.io.IOException: testing error message", storedFiles.get(0).getError());
        assertNull(storedFiles.get(1).getError());
    }

    private List<StoredFile> getDummyStoredFiles() {
        return Arrays.asList(getDummyStoredFile(), getDummyStoredFile());
    }

    private StoredFile getDummyStoredFile() {
        StoredFile storedFile = new StoredFile();
        storedFile.setFileLocation(getClass().getResource("/assertions-with-bad-url.csv").getFile()); // doesn't
        // matter
        storedFile.setOriginalFilename("original-filename.csv");
        storedFile.setDateWritten(Instant.now());
        storedFile.setOwnerId("owner");
        return storedFile;
    }

    @Test
    void testProcessAssertionUploadsWithDuplicates() throws IOException {
        Mockito.when(storedFileService.getUnprocessedStoredFilesByType(Mockito.eq(StoredFileService.ASSERTIONS_CSV_FILE_TYPE)))
            .thenReturn(Arrays.asList(getDummyStoredFile()));
        Mockito.when(assertionsUserService.getUserById(Mockito.eq("owner"))).thenReturn(getUser());

        Assertion alreadyPersisted1 = getAssertionWithEmail("1@email.com");
        alreadyPersisted1.setDepartmentName("not a duplicate");
        Assertion alreadyPersisted2 = getAssertionWithEmail("1@email.com");
        Mockito.when(assertionRepository.findByEmailAndSalesforceId(Mockito.eq("1@email.com"), Mockito.eq(DEFAULT_SALESFORCE_ID)))
            .thenReturn(Arrays.asList(alreadyPersisted1, alreadyPersisted2));

        AssertionsUpload upload = new AssertionsUpload();
        upload.addAssertion(getAssertionWithEmail("1@email.com"));
        upload.addAssertion(getAssertionWithEmail("1@email.com"));
        upload.addAssertion(getAssertionWithEmail("1@email.com"));

        Mockito.when(assertionsCsvReader.readAssertionsUpload(Mockito.any(InputStream.class), Mockito.any(AssertionServiceUser.class))).thenReturn(upload);

        assertionService.processAssertionUploads();

        Mockito.verify(mailService).sendAssertionsUploadSummaryMail(summaryCaptor.capture(), Mockito.any(AssertionServiceUser.class));
        AssertionsUploadSummary summary = summaryCaptor.getValue();

        assertEquals(3, summary.getNumDuplicates());
        assertEquals("original-filename.csv", summary.getFilename());
        assertNotNull(summary.getDate());

        Mockito.verify(assertionRepository, Mockito.never()).insert(Mockito.any(Assertion.class));
    }

    @Test
    public void testGenerateAndSendMemberAssertionStats() throws IOException {
        Mockito.when(assertionRepository.getMemberAssertionStatusCounts()).thenReturn(getDummyAssertionStatusCounts());
        Mockito.when(storedFileService.storeMemberAssertionStatsFile(Mockito.anyString())).thenReturn(new File("something"));
        Mockito.doNothing().when(mailService).sendMemberAssertionStatsMail(Mockito.any(File.class));
        Mockito.when(memberService.getMemberName(Mockito.eq("salesforceId1"))).thenReturn("member 1");
        Mockito.when(memberService.getMemberName(Mockito.eq("salesforceId2"))).thenReturn("member 2");

        assertionService.generateAndSendMemberAssertionStats();

        Mockito.verify(storedFileService).storeMemberAssertionStatsFile(csvContentCaptor.capture());
        String csv = csvContentCaptor.getValue();
        assertThat(csv).isNotNull();
        assertThat(csv).contains("member 1");
        assertThat(csv).contains("member 2");
        assertThat(csv).contains("PENDING");
        assertThat(csv).contains("IN_ORCID");

        Mockito.verify(mailService).sendMemberAssertionStatsMail(Mockito.any(File.class));
    }

    @Test
    void testUpdateAssertionsSalesforceId() {
        List<Assertion> firstPage = getAssertionsForSalesforceId("salesforce-id", 0, AssertionService.REGISTRY_SYNC_BATCH_SIZE);
        List<Assertion> secondPage = getAssertionsForSalesforceId("salesforce-id", AssertionService.REGISTRY_SYNC_BATCH_SIZE,
            AssertionService.REGISTRY_SYNC_BATCH_SIZE * 2);
        List<Assertion> thirdPage = getAssertionsForSalesforceId("salesforce-id", AssertionService.REGISTRY_SYNC_BATCH_SIZE * 2,
            (AssertionService.REGISTRY_SYNC_BATCH_SIZE * 3) - 10);
        ;
        Mockito.when(assertionRepository.findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Pageable.class))).thenReturn(new PageImpl<Assertion>(firstPage))
            .thenReturn(new PageImpl<Assertion>(secondPage)).thenReturn(new PageImpl<Assertion>(thirdPage)).thenReturn(new PageImpl<Assertion>(new ArrayList<>()));
        Mockito.when(orcidRecordService.updateTokenSalesforceIds(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

        boolean success = assertionService.updateAssertionsSalesforceId("salesforce-id", "new-salesforce-id");
        assertThat(success).isTrue();

        Mockito.verify(assertionRepository, Mockito.times((AssertionService.REGISTRY_SYNC_BATCH_SIZE * 3) - 10)).save(assertionCaptor.capture());
        Mockito.verify(orcidRecordService).updateTokenSalesforceIds(Mockito.anyString(), Mockito.anyString());
        List<Assertion> saved = assertionCaptor.getAllValues();
        saved.forEach(a -> assertThat(a.getSalesforceId()).isEqualTo("new-salesforce-id"));
    }

    @Test
    void testUpdateAssertionsSalesforceIdWithFailure() {
        List<Assertion> firstPage = getAssertionsForSalesforceId("salesforce-id", 0, AssertionService.REGISTRY_SYNC_BATCH_SIZE);
        Mockito.when(assertionRepository.findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Pageable.class))).thenReturn(new PageImpl<Assertion>(firstPage));

        // representing updated assertions that need to be rolled back
        Mockito.when(assertionRepository.findBySalesforceId(Mockito.eq("new-salesforce-id"), Mockito.any(Pageable.class)))
            .thenReturn(new PageImpl<Assertion>(Arrays.asList(new Assertion(), new Assertion(), new Assertion()))).thenReturn(new PageImpl<Assertion>(new ArrayList<>()));

        // 3 successful updates before error, followed by rollback
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenReturn(new Assertion()).thenReturn(new Assertion()).thenReturn(new Assertion())
            .thenThrow(new RuntimeException("some random problem")).thenReturn(new Assertion()).thenReturn(new Assertion()).thenReturn(new Assertion());

        boolean success = assertionService.updateAssertionsSalesforceId("salesforce-id", "new-salesforce-id");
        assertThat(success).isFalse();

        // 3 updates followed by 1 failure and 3 rollbacks
        Mockito.verify(assertionRepository, Mockito.times(7)).save(assertionCaptor.capture());
        List<Assertion> saved = assertionCaptor.getAllValues();
        assertThat(saved.get(0).getSalesforceId()).isEqualTo("new-salesforce-id");
        assertThat(saved.get(1).getSalesforceId()).isEqualTo("new-salesforce-id");
        assertThat(saved.get(2).getSalesforceId()).isEqualTo("new-salesforce-id");
        assertThat(saved.get(3).getSalesforceId()).isEqualTo("new-salesforce-id");
        assertThat(saved.get(4).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(saved.get(5).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(saved.get(6).getSalesforceId()).isEqualTo("salesforce-id");
    }

    private List<MemberAssertionStatusCount> getDummyAssertionStatusCounts() {
        List<MemberAssertionStatusCount> counts = new ArrayList<>();
        counts.add(new MemberAssertionStatusCount("salesforceId1", "PENDING", 4));
        counts.add(new MemberAssertionStatusCount("salesforceId1", "IN_ORCID", 12));
        counts.add(new MemberAssertionStatusCount("salesforceId2", "PENDING", 220));
        counts.add(new MemberAssertionStatusCount("salesforceId2", "IN_ORCID", 1));
        return counts;
    }

    private Assertion getAssertionWithoutIdForEmail(String email) {
        Assertion a = new Assertion();
        a.setEmail(email);
        a.setAffiliationSection(AffiliationSection.DISTINCTION);
        a.setDepartmentName("department");
        a.setRoleTitle("role");
        a.setStartDay("01");
        a.setStartMonth("01");
        a.setStartYear("2020");
        a.setEndDay("01");
        a.setEndMonth("01");
        a.setEndYear("2021");
        a.setOrgName("org");
        a.setOrgCountry("US");
        a.setOrgCity("city");
        a.setDisambiguationSource("RINGGOLD");
        a.setDisambiguatedOrgId("id");
        a.setExternalId("extId");
        a.setExternalIdType("extIdType");
        a.setExternalIdUrl("extIdUrl");
        a.setUrl("url");
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);
        return a;
    }

    private Optional<OrcidRecord> getOptionalOrcidRecord(int i) {
        if (i > 0 && i <= AssertionService.REGISTRY_SYNC_BATCH_SIZE / 2) {
            return Optional.empty();
        }

        // quarter with no orcid
        if (i > AssertionService.REGISTRY_SYNC_BATCH_SIZE / 2 && i <= AssertionService.REGISTRY_SYNC_BATCH_SIZE) {
            return Optional.of(new OrcidRecord());
        }

        OrcidRecord orcidRecord = new OrcidRecord();
        orcidRecord.setOrcid("orcid-" + i);
        return Optional.of(orcidRecord);
    }

    private OrcidRecord getOrcidRecord(String variant) {
        OrcidRecord record = new OrcidRecord();
        record.setOrcid("orcid" + variant);

        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken" + variant);
        tokens.add(newToken);
        record.setTokens(tokens);
        return record;
    }

    private OrcidRecord getOrcidRecordWithRevokedToken() {
        OrcidRecord record = new OrcidRecord();
        record.setOrcid("orcid");

        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken token = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken");
        token.setRevokedDate(Instant.now());
        tokens.add(token);
        record.setTokens(tokens);
        return record;
    }

    private OrcidRecord getOrcidRecordWithDeniedToken() {
        OrcidRecord record = new OrcidRecord();
        record.setOrcid("orcid");

        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken token = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken");
        token.setDeniedDate(Instant.now());
        tokens.add(token);
        record.setTokens(tokens);
        return record;
    }

    private List<Assertion> getAssertionsForCreatingInOrcid(int min, int max) {
        List<Assertion> assertions = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            Assertion assertion = getAssertionWithEmail(i + "@email.com");
            assertion.setId(String.valueOf(i));
            assertions.add(assertion);
        }
        return assertions;
    }

    private List<Assertion> getAssertionsForUpdateInOrcid(int from, int to) {
        List<Assertion> assertions = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            Assertion assertion = getAssertionWithEmailAndPutCode(i + "@email.com", String.valueOf(i));
            assertions.add(assertion);
        }
        return assertions;
    }

    private List<Assertion> getAssertionsForSalesforceId(String salesforceId, int from, int to) {
        List<Assertion> assertions = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Assertion assertion = getAssertionWithEmailAndPutCode(i + "@email.com", String.valueOf(i));
            assertion.setSalesforceId(salesforceId);
            assertions.add(assertion);
        }
        return assertions;
    }

    private Assertion getAssertionWithEmail(String email) {
        Assertion assertion = new Assertion();
        assertion.setEmail(email);
        assertion.setSalesforceId(DEFAULT_SALESFORCE_ID);
        assertion.setAffiliationSection(AffiliationSection.EMPLOYMENT);
        assertion.setDepartmentName("department");
        assertion.setOrgCity("city");
        assertion.setOrgName("org");
        assertion.setOrgRegion("region");
        assertion.setOrgCountry("us");
        assertion.setDisambiguatedOrgId("id");
        assertion.setDisambiguationSource("source");
        assertion.setStatus(AssertionStatus.PENDING.name());
        return assertion;
    }

    private Assertion getAssertionWithEmailAndPutCode(String email, String putCode) {
        Assertion assertion = new Assertion();
        assertion.setId("id" + putCode);
        assertion.setEmail(email);
        assertion.setPutCode(putCode);
        assertion.setSalesforceId(DEFAULT_SALESFORCE_ID);
        assertion.setStatus(AssertionStatus.PENDING.name());
        return assertion;
    }

    private Optional<OrcidRecord> getOptionalOrcidRecordWithIdToken() {
        OrcidRecord record = new OrcidRecord();
        record.setEmail("email");
        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken");
        tokens.add(newToken);
        record.setTokens(tokens);
        record.setOrcid("orcid");
        return Optional.of(record);
    }

    private Optional<OrcidRecord> getOptionalOrcidRecordWithoutIdToken() {
        OrcidRecord record = new OrcidRecord();
        record.setEmail("email");
        record.setOrcid("orcid");
        return Optional.of(record);
    }

    private void testUpdateStatus(Assertion skeleton, Assertion full, String expectedStatus, Optional<OrcidRecord> optionalRecord) {
        Mockito.when(assertionRepository.findById("1")).thenReturn(Optional.of(full));
        Mockito.when(assertionRepository.save(Mockito.any(Assertion.class))).thenReturn(full);
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(optionalRecord);

        assertionService.updateAssertion(skeleton, getUser());

        Mockito.verify(assertionRepository, Mockito.atLeastOnce()).save(assertionCaptor.capture());
        Assertion updated = assertionCaptor.getValue();
        assertNotNull(updated.getStatus());
        assertEquals(expectedStatus, updated.getStatus());
    }

    private void testCreateStatus(Assertion assertion, String expectedStatus, Optional<OrcidRecord> optionalRecord) {
        Mockito.when(assertionRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
            @Override
            public Assertion answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Assertion assertion = (Assertion) args[0];
                assertion.setId("1");
                return assertion;
            }
        });

        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(optionalRecord);
        assertion = assertionService.createAssertion(assertion, getUser());
        assertNotNull(assertion.getStatus());
        assertEquals(expectedStatus, assertion.getStatus());
    }
}
