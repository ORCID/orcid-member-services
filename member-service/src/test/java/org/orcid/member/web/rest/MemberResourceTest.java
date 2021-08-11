package org.orcid.member.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class MemberResourceTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberResource memberResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
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

    @Test
    public void testGetAllMembers() {
        Mockito.when(memberService.getMembers(Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(getMember(), getMember(), getMember(), getMember())));
        Mockito.when(memberService.getMembers(Mockito.any(Pageable.class), Mockito.anyString())).thenReturn(new PageImpl<>(Arrays.asList(getMember(), getMember())));

        ResponseEntity<List<Member>> response = memberResource.getAllMembers("", Mockito.mock(Pageable.class));
        assertNotNull(response);
        List<Member> members = response.getBody();
        assertEquals(4, members.size());
        Mockito.verify(memberService, Mockito.times(1)).getMembers(Mockito.any(Pageable.class));

        response = memberResource.getAllMembers("some-filter", Mockito.mock(Pageable.class));
        assertNotNull(response);
        members = response.getBody();
        assertEquals(2, members.size());
        Mockito.verify(memberService, Mockito.times(1)).getMembers(Mockito.any(Pageable.class), Mockito.anyString());
    }

    private MemberValidation getMemberValidation() {
        MemberValidation validation = new MemberValidation();
        validation.setValid(true);
        validation.setErrors(Arrays.asList("some-error"));
        return validation;
    }

    private Member getMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setClientId("XXXX-XXXX-XXXX-XXXX");
        member.setClientName("clientname");
        member.setIsConsortiumLead(false);
        member.setSalesforceId("two");
        member.setParentSalesforceId("some parent");
        return member;
    }
}
