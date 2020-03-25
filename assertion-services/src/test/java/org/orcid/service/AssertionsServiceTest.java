package org.orcid.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.client.OrcidAPIClient;
import org.orcid.domain.Assertion;
import org.orcid.repository.AssertionsRepository;
import org.orcid.security.UaaUserUtils;
import org.orcid.service.assertions.report.impl.AssertionsCSVReportWriter;

class AssertionsServiceTest {

	@Mock
	private AssertionsCSVReportWriter assertionsReportWriter;

	@Mock
	private AssertionsRepository assertionsRepository;

	@Mock
	private OrcidRecordService orcidRecordService;

	@Mock
	private OrcidAPIClient orcidAPIClient;

	@Mock
	private UaaUserUtils uaaUserUtils;

	@InjectMocks
	private AssertionsService assertionsService;

	@BeforeEach
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);
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
		a.setOwnerId("id");
		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(uaaUserUtils.getAuthenticatedUaaUserId()).thenReturn("id");
		assertionsService.createOrUpdateAssertion(a);
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
		
		Assertion b = new Assertion();
		b.setOwnerId("id");
		Mockito.when(uaaUserUtils.getAuthenticatedUaaUserId()).thenReturn("id");
		assertionsService.createOrUpdateAssertion(b);
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(b));
	}
	
	
	@Test
	void testCreateOrUpdateAssertions() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setOwnerId("id");
		
		Assertion b = new Assertion();
		b.setOwnerId("id");
		
		Assertion c = new Assertion();
		c.setId("2");
		c.setOwnerId("id");
		
		Assertion d = new Assertion();
		d.setOwnerId("id");
		
		Assertion e = new Assertion();
		e.setId("3");
		e.setOwnerId("id");
		
		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(assertionsRepository.findById("2")).thenReturn(Optional.of(c));
		Mockito.when(assertionsRepository.findById("3")).thenReturn(Optional.of(e));
		
		Mockito.when(uaaUserUtils.getAuthenticatedUaaUserId()).thenReturn("id");
		assertionsService.createOrUpdateAssertions(Arrays.asList(a, b, c, d, e));
		
		Mockito.verify(assertionsRepository, Mockito.times(3)).save(Mockito.any(Assertion.class));
		Mockito.verify(assertionsRepository, Mockito.times(2)).insert(Mockito.any(Assertion.class));
	}
	
	@Test
	void testCreateAssertion() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setOwnerId("id");
		assertionsService.createAssertion(a);
		Mockito.verify(assertionsRepository, Mockito.times(1)).insert(Mockito.eq(a));
	}
	
	@Test
	void testUpdateAssertion() {
		Assertion a = new Assertion();
		a.setId("1");
		a.setOwnerId("id");
		Mockito.when(assertionsRepository.findById("1")).thenReturn(Optional.of(a));
		Mockito.when(uaaUserUtils.getAuthenticatedUaaUserId()).thenReturn("id");
		assertionsService.createOrUpdateAssertion(a);
		Mockito.verify(assertionsRepository, Mockito.times(1)).save(Mockito.eq(a));
	}

}
