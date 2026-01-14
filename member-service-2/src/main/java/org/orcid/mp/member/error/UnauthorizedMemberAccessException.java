package org.orcid.mp.member.error;

public class UnauthorizedMemberAccessException extends Exception {

    public UnauthorizedMemberAccessException(String userEmail, String memberSalesforceId) {
        super("Unauthorized attempt by user " + userEmail + " to access member " + memberSalesforceId);
    }
}