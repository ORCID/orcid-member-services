package org.orcid.member.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.member.domain.Member;
import org.orcid.member.service.MemberService;
import org.orcid.member.validation.MemberValidation;
import org.springframework.http.ResponseEntity;

public class MemberResourceTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberResource memberResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValidateMember() throws URISyntaxException, JSONException {
        Mockito.when(memberService.validateMember(Mockito.any(Member.class))).thenReturn(getMemberValidation());
        ResponseEntity<MemberValidation> validationResponse = memberResource.validateMember(new Member());

        // always 200, even if invalid member; the request to validate is valid
        assertEquals(200, validationResponse.getStatusCodeValue());
        assertTrue(validationResponse.getBody().isValid());
        assertEquals(1, validationResponse.getBody().getErrors().size());
    }

    private MemberValidation getMemberValidation() {
        MemberValidation validation = new MemberValidation();
        validation.setValid(true);
        validation.setErrors(Arrays.asList("some-error"));
        return validation;
    }
}
