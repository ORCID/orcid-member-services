package org.orcid.memberportal.service.assertion.repository;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.SendNotificationsRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SendNotificationsRequestRepository extends MongoRepository<SendNotificationsRequest, String> {
    
    @Query("{ salesforceId: ?0, dateCompleted: null }")
    List<SendNotificationsRequest> findActiveRequestBySalesforceId(String salesforceId);

    @Query("{ dateCompleted: null }")
    List<SendNotificationsRequest> findActiveRequests();
    
}
