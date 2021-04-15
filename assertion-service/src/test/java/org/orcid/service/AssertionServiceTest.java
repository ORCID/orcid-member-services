package org.orcid.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.client.OrcidAPIClient;
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.repository.AssertionsRepository;
import org.orcid.service.assertions.download.impl.AssertionsForEditCsvWriter;
import org.orcid.service.assertions.download.impl.AssertionsReportCsvWriter;
import org.orcid.service.assertions.download.impl.PermissionLinksCsvWriter;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.springframework.data.domain.Sort;

class AssertionServiceTest {

	private static final String DEFAULT_JHI_USER_ID = "user-id";

	private static final String DEFAULT_LOGIN = "user@orcid.org";

	private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

	@Mock
	private AssertionsReportCsvWriter assertionsReportWriter;

	@Mock
	private AssertionsForEditCsvWriter assertionsForEditCsvWriter;

	@Mock
	private PermissionLinksCsvWriter permissionLinksCsvWriter;

	@Mock
	private AssertionsRepository assertionsRepository;

	@Mock
	private OrcidRecordService orcidRecordService;

	@Mock
	private OrcidAPIClient orcidAPIClient;

	@Mock
	private UserService assertionsUserService;

	@InjectMocks
	private AssertionService assertionService;

	@BeforeEach
	public void setUp() throws JSONException {
		MockitoAnnotations.initMocks(this);
		when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
		when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
	}

	private AssertionServiceUser getUser() {
		AssertionServiceUser user = new AssertionServiceUser();
		user.setId(DEFAULT_JHI_USER_ID);
		user.setLogin(DEFAULT_LOGIN);
		user.setSalesforceId(DEFAULT_SALESFORCE_ID);
		return user;
	}

	@Test
	void testAssertionExists() {
		when(assertionsRepository.existsById(Mockito.eq("exists"))).thenReturn(true);
		when(assertionsRepository.existsById(Mockito.eq("doesn't exist"))).thenReturn(false);

		assertTrue(assertionService.assertionExists("exists"));
		verify(assertionsRepository).existsById(Mockito.eq("exists"));

		assertFalse(assertionService.assertionExists("doesn't exist"));
		verify(assertionsRepository).existsById(Mockito.eq("doesn't exist"));
	}

	@Test
	void testCreateUpdateOrDelete() throws JSONException, ClientProtocolException, IOException {
		Assertion a = new Assertion();
		a.setId("1");
		a.setEmail("email");
		a.setOwnerId(DEFAULT_JHI_USER_ID);
		a.setSalesforceId(DEFAULT_SALESFORCE_ID);

		Assertion b = new Assertion();
		b.setOwnerId(DEFAULT_JHI_USER_ID);
		b.setEmail("email");
		a.setSalesforceId(DEFAULT_SALESFORCE_ID);
		
		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				return assertion;
			}
		});
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email")))
				.thenReturn(getOptionalOrcidRecordWithIdToken());
		Mockito.when(assertionsRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				assertion.setStatus("PENDING");
				return assertion;
			}
		});

		assertionService.createUpdateOrDeleteAssertion(a);
		assertNotNull(a.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));

		assertionService.createUpdateOrDeleteAssertion(b);
		assertNotNull(b.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(b));
	}	
	
	@Test
	void testCreateUpdateOrDelete_successfulDelete() throws JSONException, ClientProtocolException, IOException {
		Assertion c = new Assertion();
		c.setId("id-to-delete");
		
		Assertion cWithMoreInfo = new Assertion();
		cWithMoreInfo.setId("id-to-delete");
		cWithMoreInfo.setSalesforceId("salesforce-id");
		cWithMoreInfo.setEmail("test@orcid.org");
		
		Mockito.when(assertionsRepository.findById("id-to-delete")).thenReturn(Optional.of(cWithMoreInfo));
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithIdToken());
		Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString())).thenReturn("access-token");
		
		assertionService.createUpdateOrDeleteAssertion(c);
		assertNotNull(c.getDeletedFromORCID() != null);
		Mockito.verify(assertionsRepository, Mockito.times(1)).deleteById(Mockito.eq("id-to-delete"));
		Mockito.verify(orcidAPIClient, Mockito.times(1)).deleteAffiliation(Mockito.eq("orcid"), Mockito.eq("access-token"), Mockito.any(Assertion.class));
	}
	
	@Test
	void testCreateUpdateOrDelete_unsuccessfulDelete() throws JSONException, ClientProtocolException, IOException {
		Assertion c = new Assertion();
		c.setId("id-to-delete");
		
		Assertion cWithMoreInfo = new Assertion();
		cWithMoreInfo.setId("id-to-delete");
		cWithMoreInfo.setSalesforceId("salesforce-id");
		cWithMoreInfo.setEmail("test@orcid.org");
		
		Mockito.when(assertionsRepository.findById("id-to-delete")).thenReturn(Optional.of(cWithMoreInfo));
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org"))).thenReturn(getOptionalOrcidRecordWithoutIdToken());
		
		assertionService.createUpdateOrDeleteAssertion(c);
		assertNotNull(c.getDeletedFromORCID() != null);
		Mockito.verify(assertionsRepository, Mockito.times(1)).deleteById(Mockito.eq("id-to-delete"));
		Mockito.verify(orcidAPIClient, Mockito.never()).deleteAffiliation(Mockito.eq("orcid"), Mockito.eq("access-token"), Mockito.any(Assertion.class));
	}

	@Test
	void testCreateAssertion() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setEmail("email");
		a.setOwnerId(DEFAULT_JHI_USER_ID);

		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString()))
				.thenReturn(getOptionalOrcidRecordWithIdToken());

		Mockito.when(assertionsRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				assertion.setStatus("PENDING");
				return assertion;
			}
		});

		assertionService.createAssertion(a);
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(a));

		assertEquals("orcid", a.getOrcidId());
	}

	@Test
	void testCreateAssertionNoIdToken() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setEmail("email");
		a.setOwnerId(DEFAULT_JHI_USER_ID);

		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString()))
				.thenReturn(getOptionalOrcidRecordWithoutIdToken());

		Mockito.when(assertionsRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				assertion.setStatus("PENDING");
				return assertion;
			}
		});

		assertionService.createAssertion(a);
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(a));

	}

	@Test
	void testUpdateAssertion() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setEmail("email");
		a.setOwnerId(DEFAULT_JHI_USER_ID);
		a.setSalesforceId(DEFAULT_SALESFORCE_ID);
		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				assertion.setStatus("PENDING");
				return assertion;
			}
		});
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email")))
				.thenReturn(getOptionalOrcidRecordWithIdToken());
		a = assertionService.updateAssertion(a);
		assertNotNull(a.getStatus());
		assertEquals("orcid", a.getOrcidId());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
	}

	@Test
	void testUpdateAssertionNoIdToken() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setEmail("email");
		a.setOwnerId(DEFAULT_JHI_USER_ID);
		a.setSalesforceId(DEFAULT_SALESFORCE_ID);

		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setStatus("PENDING");
				assertion.setId("12345");
				return assertion;
			}
		});
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email")))
				.thenReturn(getOptionalOrcidRecordWithoutIdToken());
		a = assertionService.updateAssertion(a);
		assertNotNull(a.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
	}

	@Test
	void testOrcidNotFetchedIfAlreadyPresent() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setEmail("email");
		a.setOrcidId("orcid-already-present");
		a.setOwnerId(DEFAULT_JHI_USER_ID);
		a.setSalesforceId(DEFAULT_SALESFORCE_ID);
		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				assertion.setStatus("PENDING");
				return assertion;
			}
		});
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email")))
				.thenReturn(getOptionalOrcidRecordWithIdToken());
		a = assertionService.updateAssertion(a);
		assertNotNull(a.getStatus());
		assertEquals("orcid-already-present", a.getOrcidId());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
	}

	@Test
	void checkErrorWhereNoEmailInAssertion() {
		// assertion with no email
		Assertion a = new Assertion();
		a.setId("1");
		a.setOwnerId(DEFAULT_JHI_USER_ID);
		a.setSalesforceId(DEFAULT_SALESFORCE_ID);

		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.save(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				return assertion;
			}
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			assertionService.updateAssertion(a);
		});
	}

	@Test
	void testPostAssertionsToOrcid()
			throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
		Mockito.when(assertionsRepository.findAllToCreate()).thenReturn(getAssertionsForCreatingInOrcid());
		for (int i = 1; i <= 20; i++) {
			Mockito.when(orcidRecordService.findOneByEmail(i + "@email.com")).thenReturn(getOptionalOrcidRecord(i));
		}

		for (int i = 16; i <= 20; i++) {
			Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken" + i))).thenReturn("accessToken" + i);
			Mockito.when(orcidAPIClient.postAffiliation(Mockito.eq("orcid" + i), Mockito.eq("accessToken" + i),
					Mockito.any(Assertion.class))).thenReturn("putCode" + i);
		}

		assertionService.postAssertionsToOrcid();

		Mockito.verify(orcidRecordService, Mockito.times(20)).findOneByEmail(Mockito.anyString());
		// Mockito.verify(orcidAPIClient,
		// Mockito.times(5)).exchangeToken(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(5)).postAffiliation(Mockito.anyString(), Mockito.anyString(),
				Mockito.any(Assertion.class));
	}

	@Test
	void testPutAssertionsToOrcid() throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
		Mockito.when(assertionsRepository.findAllToUpdate()).thenReturn(getAssertionsForUpdateInOrcid());
		for (int i = 1; i <= 20; i++) {
			Mockito.when(orcidRecordService.findOneByEmail(i + "@email.com")).thenReturn(getOptionalOrcidRecord(i));
		}

		for (int i = 16; i <= 20; i++) {
			Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken" + i))).thenReturn("accessToken" + i);
			Mockito.when(orcidAPIClient.postAffiliation(Mockito.eq("orcid" + i), Mockito.eq("accessToken" + i),
					Mockito.any(Assertion.class))).thenReturn("putCode" + i);
		}

		assertionService.putAssertionsToOrcid();

		Mockito.verify(orcidRecordService, Mockito.times(25)).findOneByEmail(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(5)).exchangeToken(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(5)).putAffiliation(Mockito.anyString(), Mockito.anyString(),
				Mockito.any(Assertion.class));
	}

	@Test
	void testFindByEmail() {
		String email = "email@email.com";
		Assertion assertion = getAssertionWithEmail(email);
		Mockito.when(assertionsRepository.findByEmail(Mockito.eq(email))).thenReturn(Arrays.asList(assertion));

		List<Assertion> assertions = assertionService.findByEmail(email);
		assertFalse(assertions.isEmpty());
		assertEquals(1, assertions.size());

		Mockito.verify(assertionsRepository, Mockito.times(1)).findByEmail(Mockito.eq(email));
	}

	@Test
	void testGenerateAssertionsCSV() throws IOException {
		when(assertionsForEditCsvWriter.writeCsv()).thenReturn("test");
		String csv = assertionService.generateAssertionsCSV();
		assertEquals("test", csv);
		verify(assertionsForEditCsvWriter, times(1)).writeCsv();
	}

	@Test
	void testGenerateAssertionsReport() throws IOException {
		Mockito.when(assertionsReportWriter.writeCsv()).thenReturn("test");
		String csv = assertionService.generateAssertionsReport();
		assertEquals("test", csv);
		Mockito.verify(assertionsReportWriter, Mockito.times(1)).writeCsv();
	}

	@Test
	void testGenerateLinks() throws IOException {
		Mockito.when(permissionLinksCsvWriter.writeCsv()).thenReturn("test");
		String csv = assertionService.generatePermissionLinks();
		assertEquals("test", csv);
		Mockito.verify(permissionLinksCsvWriter, Mockito.times(1)).writeCsv();
	}

	@Test
	void testDeleteById_recordWithMultipleAssertions() {
		Assertion assertion = getAssertionWithEmail("test@orcid.org");
		assertion.setId("id");
		Mockito.when(assertionsRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));

		List<Assertion> listOfAssertions = Arrays.asList(getAssertionWithEmail("whatever@orcid.org"));
		Mockito.when(assertionsRepository.findByEmail("test@orcid.org")).thenReturn(listOfAssertions);

		Mockito.when(orcidRecordService.generateLinkForEmail("test@orcid.org")).thenReturn("don't care");
		Mockito.doNothing().when(assertionsRepository).deleteById(Mockito.eq("id"));

		assertionService.deleteById("id");

		Mockito.verify(assertionsRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
		Mockito.verify(orcidRecordService, Mockito.never()).deleteOrcidRecord(Mockito.any());
	}

	@Test
	void testDeleteById_recordWithNoOtherAssertions() {
		Assertion assertion = getAssertionWithEmail("test@orcid.org");
		assertion.setId("id");
		Mockito.when(assertionsRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
		Mockito.when(assertionsRepository.findByEmail(Mockito.eq("test@orcid.org"))).thenReturn(new ArrayList<>());
		Mockito.when(orcidRecordService.generateLinkForEmail(Mockito.eq("test@orcid.org"))).thenReturn("don't care");
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@orcid.org")))
				.thenReturn(getOptionalOrcidRecordWithoutIdToken());
		Mockito.doNothing().when(orcidRecordService).deleteOrcidRecord(Mockito.any(OrcidRecord.class));
		Mockito.doNothing().when(assertionsRepository).deleteById(Mockito.eq("id"));

		assertionService.deleteById("id");

		Mockito.verify(assertionsRepository, Mockito.times(1)).deleteById(Mockito.eq("id"));
		Mockito.verify(orcidRecordService, Mockito.times(1)).deleteOrcidRecord(Mockito.any());
	}

	@Test
	void testDeleteAllBySalesforceId_orcidRecordsDeleted() {
		Mockito.when(assertionsRepository.findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Sort.class)))
				.thenReturn(getAssertionsForUpdateInOrcid());
		Mockito.doNothing().when(assertionsRepository).deleteById(Mockito.anyString());
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(Optional.empty());

		assertionService.deleteAllBySalesforceId("salesforce-id");

		Mockito.verify(assertionsRepository, Mockito.times(1)).findBySalesforceId(Mockito.eq("salesforce-id"),
				Mockito.any(Sort.class));
		Mockito.verify(assertionsRepository, Mockito.times(20)).deleteById(Mockito.anyString());
		Mockito.verify(orcidRecordService, Mockito.never()).deleteOrcidRecord(Mockito.any(OrcidRecord.class));
	}

	@Test
	void testDeleteAllBySalesforceId_orcidRecordsNotDeleted() {
		Mockito.when(assertionsRepository.findBySalesforceId(Mockito.eq("salesforce-id"), Mockito.any(Sort.class)))
				.thenReturn(getAssertionsForUpdateInOrcid());
		Mockito.doNothing().when(assertionsRepository).deleteById(Mockito.anyString());
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString()))
				.thenReturn(getOptionalOrcidRecordWithoutIdToken());

		assertionService.deleteAllBySalesforceId("salesforce-id");

		Mockito.verify(assertionsRepository, Mockito.times(1)).findBySalesforceId(Mockito.eq("salesforce-id"),
				Mockito.any(Sort.class));
		Mockito.verify(assertionsRepository, Mockito.times(20)).deleteById(Mockito.anyString());
		Mockito.verify(orcidRecordService, Mockito.times(20)).deleteOrcidRecord(Mockito.any(OrcidRecord.class));
	}

	@Test
	void testDeleteAssertionFromOrcidRegistry_deleteSuccessful()
			throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
		Assertion assertion = getAssertionWithEmailAndPutCode("test@email.com", "1001");
		assertion.setSalesforceId("salesforce-id");

		Mockito.when(assertionsRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
		Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@email.com")))
				.thenReturn(getOptionalOrcidRecordWithIdToken());
		Mockito.when(orcidAPIClient.exchangeToken(Mockito.anyString())).thenReturn("exchange-token");
		Mockito.when(orcidAPIClient.deleteAffiliation(Mockito.anyString(), Mockito.eq("exchange-token"),
				Mockito.any(Assertion.class))).thenReturn(true);

		assertionService.deleteAssertionFromOrcidRegistry("id");

		Mockito.verify(assertionsRepository, Mockito.times(1)).findById(Mockito.eq("id"));
		Mockito.verify(assertionsUserService, Mockito.times(1)).getLoggedInUserSalesforceId();
		Mockito.verify(orcidRecordService, Mockito.atLeastOnce()).findOneByEmail(Mockito.eq("test@email.com"));
		Mockito.verify(orcidAPIClient, Mockito.times(1)).exchangeToken(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(1)).deleteAffiliation(Mockito.anyString(),
				Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
	}

	@Test
	void testDeleteAssertionFromOrcidRegistry_wrongSalesforceId()
			throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
		Assertion assertion = getAssertionWithEmailAndPutCode("test@email.com", "1001");
		assertion.setSalesforceId("salesforce-id");
		Mockito.when(assertionsRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
		Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("wrong-salesforce-id");

		Assertions.assertThrows(BadRequestAlertException.class, () -> {
			assertionService.deleteAssertionFromOrcidRegistry("id");
		});
	}

	@Test
	void testDeleteAssertionFromOrcidRegistry_CannotDelete()
			throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
		Assertion assertion = getAssertionWithEmailAndPutCode("test@email.com", "1001");
		assertion.setSalesforceId("salesforce-id");

		Mockito.when(assertionsRepository.findById(Mockito.eq("id"))).thenReturn(Optional.of(assertion));
		Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforce-id");
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("test@email.com")))
				.thenReturn(getOptionalOrcidRecordWithoutIdToken());

		assertionService.deleteAssertionFromOrcidRegistry("id");

		Mockito.verify(assertionsRepository, Mockito.times(1)).findById(Mockito.eq("id"));
		Mockito.verify(assertionsUserService, Mockito.times(1)).getLoggedInUserSalesforceId();
		Mockito.verify(orcidRecordService, Mockito.atLeastOnce()).findOneByEmail(Mockito.eq("test@email.com"));
		Mockito.verify(orcidAPIClient, Mockito.never()).exchangeToken(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.never()).deleteAffiliation(Mockito.anyString(),
				Mockito.eq("exchange-token"), Mockito.any(Assertion.class));
	}
	
	@Test
	void testIsDuplicate() {
		Assertion a = getAssertionWithoutIdForEmail("email");
		Assertion b = getAssertionWithoutIdForEmail("email");
		b.setRoleTitle("something different");
		Assertion c = getAssertionWithoutIdForEmail("email");
		c.setEndDay("09");
		Assertion d = getAssertionWithoutIdForEmail("email");
		d.setAffiliationSection(AffiliationSection.EMPLOYMENT);
		
		Assertion comparison = getAssertionWithoutIdForEmail("email"); // duplicate of assertion a
		Mockito.when(assertionsRepository.findByEmail(Mockito.eq("email"))).thenReturn(Arrays.asList(b, c, a, d));
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setUrl("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setUrl(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setUrl("url");
		assertTrue(assertionService.isDuplicate(comparison));
		
		comparison.setUrl(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		comparison.setUrl("url");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setStartMonth("08");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setStartMonth(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setStartMonth("01");
		assertTrue(assertionService.isDuplicate(comparison));

		comparison.setStartMonth(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		comparison.setStartMonth("01");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setStartYear("1981");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setStartYear(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setStartYear("2020");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setEndDay("02");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setEndDay(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setEndDay("01");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setEndMonth("06");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setEndMonth(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setEndMonth("01");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setEndYear("1981");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setEndYear(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setEndYear("2021");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setOrgName("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setOrgName(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setOrgName("org");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setOrgCity("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setOrgCity(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setOrgCity("city");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setDisambiguationSource("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setDisambiguationSource(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setDisambiguationSource("RINGGOLD");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setDisambiguatedOrgId("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setDisambiguatedOrgId(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setDisambiguatedOrgId("id");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setExternalId("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setExternalId(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setExternalId("extId");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setExternalIdType("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setExternalIdType(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setExternalIdType("extIdType");
		assertTrue(assertionService.isDuplicate(comparison));
		
		a.setExternalIdUrl("something-different");
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setExternalIdUrl(null);
		assertFalse(assertionService.isDuplicate(comparison));
		
		a.setExternalIdUrl("extIdUrl");
		assertTrue(assertionService.isDuplicate(comparison));

		comparison.setId("not-null");
		assertFalse(assertionService.isDuplicate(comparison));
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
		return a;
	}

	private Optional<OrcidRecord> getOptionalOrcidRecord(int i) {
		// quarter without orcid record
		if (i > 0 && i <= 5) {
			return Optional.empty();
		}

		// quarter with no orcid
		if (i > 5 && i <= 10) {
			return Optional.of(new OrcidRecord());
		}

		// quarter with no id token
		if (i > 10 && i <= 15) {
			OrcidRecord record = new OrcidRecord();
			record.setOrcid("orcid" + i);
			return Optional.of(record);
		}

		// quarter with id token and orcid
		if (i > 15 && i <= 20) {
			OrcidRecord record = new OrcidRecord();
			record.setOrcid("orcid" + i);

			List<OrcidToken> tokens = new ArrayList<OrcidToken>();
			OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken" + i, null, null);
			tokens.add(newToken);
			record.setTokens(tokens);
			return Optional.of(record);
		}

		return null;
	}

	private List<Assertion> getAssertionsForCreatingInOrcid() {
		List<Assertion> assertions = new ArrayList<>();
		for (int i = 1; i <= 20; i++) {
			assertions.add(getAssertionWithEmail(i + "@email.com"));
		}
		return assertions;
	}

	private List<Assertion> getAssertionsForUpdateInOrcid() {
		List<Assertion> assertions = new ArrayList<>();
		for (int i = 1; i <= 20; i++) {
			Assertion assertion = getAssertionWithEmailAndPutCode(i + "@email.com", String.valueOf(i));
			assertions.add(assertion);
		}
		return assertions;
	}

	private Assertion getAssertionWithEmail(String email) {
		Assertion assertion = new Assertion();
		assertion.setId("id");
		assertion.setEmail(email);
		assertion.setSalesforceId(DEFAULT_SALESFORCE_ID);
		return assertion;
	}

	private Assertion getAssertionWithEmailAndPutCode(String email, String putCode) {
		Assertion assertion = new Assertion();
		assertion.setId("id");
		assertion.setEmail(email);
		assertion.setPutCode(putCode);
		assertion.setSalesforceId(DEFAULT_SALESFORCE_ID);
		return assertion;
	}

	private Optional<OrcidRecord> getOptionalOrcidRecordWithIdToken() {
		OrcidRecord record = new OrcidRecord();
		record.setEmail("email");
		List<OrcidToken> tokens = new ArrayList<OrcidToken>();
		OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken", null, null);
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

}
