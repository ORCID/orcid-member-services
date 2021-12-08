package org.orcid.memberportal.service.assertion.repository;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredFileRepository extends MongoRepository<StoredFile, String> {
    
    @Query("{fileType: ?0, dateProcessed: null}")
    List<StoredFile> findUnprocessedByType(String type);

}
