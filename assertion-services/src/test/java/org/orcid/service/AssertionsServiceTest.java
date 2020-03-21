package org.orcid.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.client.OrcidAPIClient;
import org.orcid.repository.AssertionsRepository;
import org.orcid.security.UaaUserUtils;
import org.orcid.service.report.impl.AssertionsCSVReportWriter;

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

}
