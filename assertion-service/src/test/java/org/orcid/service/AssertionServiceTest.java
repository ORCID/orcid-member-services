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
import org.orcid.repository.AssertionsRepository;
import org.orcid.service.assertions.download.impl.AssertionsForEditCsvWriter;
import org.orcid.service.assertions.download.impl.AssertionsReportCsvWriter;

class AssertionServiceTest {

	private static final String DEFAULT_JHI_USER_ID = "user-id";

	private static final String DEFAULT_LOGIN = "user@orcid.org";

	private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

	@Mock
	private AssertionsReportCsvWriter assertionsReportWriter;
	
	@Mock
	private AssertionsForEditCsvWriter assertionsForEditCsvWriter;

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
	void testCreateOrUpdateAssertion() {
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
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
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

		a = assertionService.createOrUpdateAssertion(a);
		assertNotNull(a.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
		assertEquals("orcid", a.getOrcidId());

		b = assertionService.createOrUpdateAssertion(b);
		assertNotNull(b.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(b));
		assertEquals("orcid", b.getOrcidId());
	}

	@Test
	void testCreateOrUpdateAssertionNoIdToken() {
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
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithoutIdToken());
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

		a = assertionService.createOrUpdateAssertion(a);
		assertNotNull(a.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
		

		b = assertionService.createOrUpdateAssertion(b);
		assertNotNull(b.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(b));
        
	}

	@Test
	void testCreateOrUpdateAssertions() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setOwnerId(DEFAULT_JHI_USER_ID);
		a.setEmail("email");
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);

		Assertion b = new Assertion();
		b.setOwnerId(DEFAULT_JHI_USER_ID);
		b.setEmail("email");
        b.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion c = new Assertion();
		c.setId("2");
		c.setOwnerId(DEFAULT_JHI_USER_ID);
		c.setEmail("email");
        c.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion d = new Assertion();
		d.setOwnerId(DEFAULT_JHI_USER_ID);
		d.setEmail("email");
        d.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion e = new Assertion();
		e.setId("3");
		e.setOwnerId(DEFAULT_JHI_USER_ID);
		e.setEmail("email");
        e.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.findById("2")).thenReturn(Optional.of(c));
		Mockito.when(assertionsRepository.findById("3")).thenReturn(Optional.of(e));
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getOptionalOrcidRecordWithIdToken());
		Mockito.when(assertionsRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				return assertion;
			}
		});
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

		assertionService.createOrUpdateAssertions(Arrays.asList(a, b, c, d, e));

		Mockito.verify(assertionsRepository, Mockito.times(3)).save(Mockito.any(Assertion.class));
		Mockito.verify(assertionsRepository, Mockito.times(2)).insert(Mockito.any(Assertion.class));

		assertEquals("orcid", a.getOrcidId());
		assertEquals("orcid", b.getOrcidId());
		assertEquals("orcid", c.getOrcidId());
		assertEquals("orcid", d.getOrcidId());
		assertEquals("orcid", e.getOrcidId());
	}

	@Test
	void testCreateOrUpdateAssertionsNoIdTokens() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setOwnerId(DEFAULT_JHI_USER_ID);
		a.setEmail("email");
        a.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion b = new Assertion();
		b.setOwnerId(DEFAULT_JHI_USER_ID);
		b.setEmail("email");
        b.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion c = new Assertion();
		c.setId("2");
		c.setOwnerId(DEFAULT_JHI_USER_ID);
		c.setEmail("email");
        c.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion d = new Assertion();
		d.setOwnerId(DEFAULT_JHI_USER_ID);
		d.setEmail("email");
        d.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Assertion e = new Assertion();
		e.setId("3");
		e.setOwnerId(DEFAULT_JHI_USER_ID);
		e.setEmail("email");
        e.setSalesforceId(DEFAULT_SALESFORCE_ID);

        Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.findById("2")).thenReturn(Optional.of(c));
		Mockito.when(assertionsRepository.findById("3")).thenReturn(Optional.of(e));
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getOptionalOrcidRecordWithoutIdToken());
		Mockito.when(assertionsRepository.insert(Mockito.any(Assertion.class))).thenAnswer(new Answer<Assertion>() {
			@Override
			public Assertion answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				Assertion assertion = (Assertion) args[0];
				assertion.setId("12345");
				return assertion;
			}
		});
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

		assertionService.createOrUpdateAssertions(Arrays.asList(a, b, c, d, e));

		Mockito.verify(assertionsRepository, Mockito.times(3)).save(Mockito.any(Assertion.class));
		Mockito.verify(assertionsRepository, Mockito.times(2)).insert(Mockito.any(Assertion.class));

	}

	@Test
	void testCreateAssertion() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setEmail("email");
		a.setOwnerId(DEFAULT_JHI_USER_ID);

		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getOptionalOrcidRecordWithIdToken());

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

		Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getOptionalOrcidRecordWithoutIdToken());

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
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
		a = assertionService.createOrUpdateAssertion(a);
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
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithoutIdToken());
		a = assertionService.createOrUpdateAssertion(a);
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
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
		a = assertionService.createOrUpdateAssertion(a);
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
			assertionService.createOrUpdateAssertion(a);
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
		//Mockito.verify(orcidAPIClient, Mockito.times(5)).exchangeToken(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(5)).postAffiliation(Mockito.anyString(), Mockito.anyString(),
				Mockito.any(Assertion.class));
	}


	@Test
	void testPutAssertionsToOrcid()
			throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
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
		Mockito.verify(orcidAPIClient, Mockito.times(5)).putAffiliation(Mockito.anyString(), Mockito.anyString(),Mockito.any(Assertion.class));
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
			assertions.add(getAssertionWithEmailAndPutCode(i + "@email.com"));
		}
		return assertions;
	}

	private Assertion getAssertionWithEmail(String email) {
		Assertion assertion = new Assertion();
		assertion.setEmail(email);
		assertion.setSalesforceId(DEFAULT_SALESFORCE_ID);
		return assertion;
	}
	
	private Assertion getAssertionWithEmailAndPutCode(String email) {
		Assertion assertion = new Assertion();
		assertion.setEmail(email);
		assertion.setPutCode("1234");
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
