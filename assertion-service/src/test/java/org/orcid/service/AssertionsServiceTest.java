package org.orcid.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.apache.http.client.ClientProtocolException;
import org.codehaus.jettison.json.JSONException;
import org.json.JSONObject;
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
import org.orcid.security.SecurityUtils;
import org.orcid.service.assertions.report.impl.AssertionsCSVReportWriter;

class AssertionsServiceTest {

	private static final String DEFAULT_JHI_USER_ID = "user-id";

	private static final String DEFAULT_LOGIN = "user@orcid.org";

	private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";
	

	@Mock
	private AssertionsCSVReportWriter assertionsReportWriter;

	@Mock
	private AssertionsRepository assertionsRepository;

	@Mock
	private OrcidRecordService orcidRecordService;

	@Mock
	private OrcidAPIClient orcidAPIClient;

	@Mock
	private UserService assertionsUserService;

	@InjectMocks
	private AssertionService assertionsService;

	@BeforeEach
	public void setUp() throws JSONException {
		MockitoAnnotations.initMocks(this);
		when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
		when(assertionsUserService.getLoggedInUserId()).thenReturn(getUser().getId());
	}

	private AssertionServiceUser getUser() {
		AssertionServiceUser user = new AssertionServiceUser();
		user.setId(DEFAULT_JHI_USER_ID);
		user.setLogin(DEFAULT_LOGIN);
		user.setSalesforceId(DEFAULT_SALESFORCE_ID);
		return user;
	}

	@Test
	void testGenerateAssertionsReport() throws IOException {
		Mockito.when(assertionsReportWriter.writeAssertionsReport()).thenReturn("test");
		assertNotNull(assertionsService.generateAssertionsReport());
		Mockito.verify(assertionsReportWriter, Mockito.times(1)).writeAssertionsReport();
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
				return assertion;
			}
		});

		a = assertionsService.createOrUpdateAssertion(a);
		assertNotNull(a.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
		assertEquals("orcid", a.getOrcidId());

		b = assertionsService.createOrUpdateAssertion(b);
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
				return assertion;
			}
		});

		a = assertionsService.createOrUpdateAssertion(a);
		assertNotNull(a.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
		assertNull(a.getOrcidId());

		b = assertionsService.createOrUpdateAssertion(b);
		assertNotNull(b.getStatus());
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(b));
		assertNull(b.getOrcidId());
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
				return assertion;
			}
		});

		assertionsService.createOrUpdateAssertions(Arrays.asList(a, b, c, d, e));

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
				return assertion;
			}
		});

		assertionsService.createOrUpdateAssertions(Arrays.asList(a, b, c, d, e));

		Mockito.verify(assertionsRepository, Mockito.times(3)).save(Mockito.any(Assertion.class));
		Mockito.verify(assertionsRepository, Mockito.times(2)).insert(Mockito.any(Assertion.class));

		assertNull(a.getOrcidId());
		assertNull(b.getOrcidId());
		assertNull(c.getOrcidId());
		assertNull(d.getOrcidId());
		assertNull(e.getOrcidId());
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
				return assertion;
			}
		});

		assertionsService.createAssertion(a);
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
				return assertion;
			}
		});

		assertionsService.createAssertion(a);
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(a));
		assertNull(a.getOrcidId());
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
				return assertion;
			}
		});
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
		a = assertionsService.createOrUpdateAssertion(a);
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
				assertion.setId("12345");
				return assertion;
			}
		});
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithoutIdToken());
		a = assertionsService.createOrUpdateAssertion(a);
		assertNotNull(a.getStatus());
		assertNull(a.getOrcidId());
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
				return assertion;
			}
		});
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq("email"))).thenReturn(getOptionalOrcidRecordWithIdToken());
		a = assertionsService.createOrUpdateAssertion(a);
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
			assertionsService.createOrUpdateAssertion(a);
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

		assertionsService.postAssertionsToOrcid();

		Mockito.verify(orcidRecordService, Mockito.times(20)).findOneByEmail(Mockito.anyString());
		//Mockito.verify(orcidAPIClient, Mockito.times(5)).exchangeToken(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(5)).postAffiliation(Mockito.anyString(), Mockito.anyString(),
				Mockito.any(Assertion.class));
	}


	@Test
	void testPutAssertionsToOrcid()
			throws org.json.JSONException, ClientProtocolException, IOException, JAXBException {
		Mockito.when(assertionsRepository.findAllToUpdate()).thenReturn(getAssertionsForCreatingInOrcid());
		for (int i = 1; i <= 20; i++) {
			Mockito.when(orcidRecordService.findOneByEmail(i + "@email.com")).thenReturn(getOptionalOrcidRecord(i));
		}

		for (int i = 16; i <= 20; i++) {
			Mockito.when(orcidAPIClient.exchangeToken(Mockito.eq("idToken" + i))).thenReturn("accessToken" + i);
			Mockito.when(orcidAPIClient.postAffiliation(Mockito.eq("orcid" + i), Mockito.eq("accessToken" + i),
					Mockito.any(Assertion.class))).thenReturn("putCode" + i);
		}

		assertionsService.putAssertionsToOrcid();

		Mockito.verify(orcidRecordService, Mockito.times(20)).findOneByEmail(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(5)).exchangeToken(Mockito.anyString());
		Mockito.verify(orcidAPIClient, Mockito.times(5)).putAffiliation(Mockito.anyString(), Mockito.anyString(),Mockito.any(Assertion.class));
	}

	@Test
	void testFindByEmail() {
		String email = "email@email.com";
		Assertion assertion = getAssertionWithEmail(email);
		Mockito.when(assertionsRepository.findByEmail(Mockito.eq(email))).thenReturn(Arrays.asList(assertion));

		List<Assertion> assertions = assertionsService.findByEmail(email);
		assertFalse(assertions.isEmpty());
		assertEquals(1, assertions.size());

		Mockito.verify(assertionsRepository, Mockito.times(1)).findByEmail(Mockito.eq(email));
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
	                OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken" + i);
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

	private Assertion getAssertionWithEmail(String email) {
		Assertion assertion = new Assertion();
		assertion.setEmail(email);
		assertion.setSalesforceId(DEFAULT_SALESFORCE_ID);
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

}
