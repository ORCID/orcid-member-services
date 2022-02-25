package org.orcid.memberportal.service.assertion.repository;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.CsvReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvReportRepository extends MongoRepository<CsvReport, String> {
    
    @Query("{ status: '" + CsvReport.UNPROCESSED_STATUS + "' }")
    List<CsvReport> findAllUnprocessed();
    
}
