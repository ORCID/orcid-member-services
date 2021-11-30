package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.repository.StoredFileRepository;
import org.springframework.test.util.ReflectionTestUtils;

class StoredFileServiceTest {
    
    @InjectMocks
    private StoredFileService storedFileService;
    
    @Mock
    private StoredFileRepository storedFileRepository;
    
    @Captor
    private ArgumentCaptor<StoredFile> storedFileCaptor;
    
    private File storedFilesDir;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        
        storedFilesDir = File.createTempFile("test-dir", "");
        if (!storedFilesDir.delete()) {
            throw new RuntimeException("Test couldn't delete file " + storedFilesDir.getAbsolutePath());
        }
        storedFilesDir.mkdir();
        
        ApplicationProperties applicationProperties = getTestApplicationProperties();
        ReflectionTestUtils.setField(storedFileService, "applicationProperties", applicationProperties);
    }
    
    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(storedFilesDir);
    }
    
    @Test
    void testStoreMemberAssertionStatsFile() throws IOException {
        File file = storedFileService.storeMemberAssertionStatsFile("some content");
        assertThat(file.getParent()).isEqualTo(storedFilesDir.getAbsolutePath());
        Mockito.verify(storedFileRepository).save(storedFileCaptor.capture());
        StoredFile saved = storedFileCaptor.getValue();
        assertThat(saved.getFileLocation()).isNotNull();
        assertThat(saved.getFileLocation()).startsWith(storedFilesDir.getAbsolutePath());
        assertThat(saved.getFileLocation()).endsWith(".csv");
        assertThat(saved.getFileType()).isNotNull();
        assertThat(saved.getFileType()).isEqualTo(StoredFileService.MEMBER_ASSERTION_STATS_FILE_TYPE);
        assertThat(saved.getDateWritten()).isNotNull();
        assertThat(saved.getRemovalDate()).isNotNull();
        assertThat(saved.getRemovalDate().minus(7, ChronoUnit.DAYS)).isEqualTo(saved.getDateWritten());
    }

    private ApplicationProperties getTestApplicationProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setMemberAssertionStatsDirectory(storedFilesDir.getAbsolutePath());
        properties.setStoredFileLifespan(7);
        return properties;
    }
    
}
