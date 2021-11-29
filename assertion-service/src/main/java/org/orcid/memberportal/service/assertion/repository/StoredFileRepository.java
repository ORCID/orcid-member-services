package org.orcid.memberportal.service.assertion.repository;

import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredFileRepository extends MongoRepository<StoredFile, String> {

}
