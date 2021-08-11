package org.orcid.user.service;

import feign.FeignException;

import org.apache.commons.lang3.StringUtils;
import org.orcid.user.client.MemberServiceClient;
import org.orcid.user.service.member.MemberServiceMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    @Autowired
    private MemberServiceClient memberServiceClient;

    public boolean memberExistsWithSalesforceId(String salesforceId) {
        try {
            ResponseEntity<MemberServiceMember> response = memberServiceClient.getMember(salesforceId);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }

            if (response.getStatusCode().is4xxClientError()) {
                return false;
            }

        } catch (FeignException ex) {

            HttpStatus status = HttpStatus.resolve(ex.status());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            System.out.println(status.value());
            if (status.is2xxSuccessful()) {
                return true;
            }
            if (status.is4xxClientError()) {
                return false;
            }

        }

        throw new RuntimeException("Error contacting member service");
    }

    public boolean memberExistsWithSalesforceIdAndAssertionsEnabled(String salesforceId) {
        ResponseEntity<MemberServiceMember> response = memberServiceClient.getMember(salesforceId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody().getAssertionServiceEnabled();
        }
        if (response.getStatusCodeValue() == 404) {
            return false;
        }

        throw new RuntimeException("Error contacting member service");
    }

    public Boolean memberExistsWithSalesforceIdAndSuperadminEnabled(String salesforceId) {
        ResponseEntity<MemberServiceMember> response = memberServiceClient.getMember(salesforceId);
        if (response.getStatusCode().is2xxSuccessful()) {
            if (response.getBody().getSuperadminEnabled() == null) {
                return false;
            } else {
                return response.getBody().getSuperadminEnabled();
            }
        }
        if (response.getStatusCodeValue() == 404) {
            return false;
        }

        throw new RuntimeException("Error contacting member service");
    }

    public String getMemberNameBySalesforce(String salesforceId) {
        if (StringUtils.isBlank(salesforceId)) {
            return null;
        }

        ResponseEntity<MemberServiceMember> response = memberServiceClient.getMember(salesforceId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody().getClientName();
        }
        if (response.getStatusCodeValue() == 404) {
            throw new RuntimeException("Member not found");
        }

        throw new RuntimeException("Error contacting member service");
    }

    public Boolean memberIsConsortiumLead(String salesforceId) {
        ResponseEntity<MemberServiceMember> response = memberServiceClient.getMember(salesforceId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody().getIsConsortiumLead();
        }
        if (response.getStatusCodeValue() == 404) {
            throw new RuntimeException("Member not found");
        }

        throw new RuntimeException("Error contacting member service");
    }
}
