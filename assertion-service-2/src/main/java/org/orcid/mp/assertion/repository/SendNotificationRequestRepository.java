package org.orcid.mp.assertion.repository;

import java.util.List;

import org.orcid.mp.assertion.domain.SendNotificationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SendNotificationRequestRepository extends MongoRepository<SendNotificationRequest, String> {

    @Query("{ salesforceId: ?0, dateCompleted: null }")
    List<SendNotificationRequest> findActiveRequestBySalesforceId(String salesforceId);

    @Query("{ dateCompleted: null }")
    List<SendNotificationRequest> findActiveRequests();

}