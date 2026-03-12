package org.orcid.mp.assertion.client;

import org.orcid.mp.assertion.domain.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MemberServiceClient {

    @Autowired
    @Qualifier("memberServiceRestClient")
    private RestClient restClient;

    public Member getMember(String id) {
        return restClient.get().uri("/members/" + id).retrieve().toEntity(Member.class).getBody();
    }

    public String updateMemberDefaultLanguage(String salesforceId, String language) {
        return restClient.post().uri("/members/" + salesforceId + "/language/" + language).retrieve().toEntity(String.class).getBody();
    }

}
