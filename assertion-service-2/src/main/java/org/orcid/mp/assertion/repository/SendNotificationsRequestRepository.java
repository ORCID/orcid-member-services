package org.orcid.mp.assertion.repository;

import java.util.List;

import org.orcid.mp.assertion.domain.SendNotificationsRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SendNotificationsRequestRepository extends MongoRepository<SendNotificationsRequest, String> {

    @Query("{ memberId: ?0, dateCompleted: null }")
    List<SendNotificationsRequest> findActiveRequestByMemberId(String memberId);

    @Query("{ dateCompleted: null }")
    List<SendNotificationsRequest> findActiveRequests();

}