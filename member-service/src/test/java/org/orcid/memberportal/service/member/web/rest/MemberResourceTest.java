package org.orcid.memberportal.service.member.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.orcid.memberportal.service.member.client.model.MemberContact;
import org.orcid.memberportal.service.member.client.model.MemberContacts;
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.client.model.MemberOrgId;
import org.orcid.memberportal.service.member.client.model.MemberOrgIds;
import org.orcid.memberportal.service.member.client.model.PublicMemberDetails;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.services.MemberService;
import org.orcid.memberportal.service.member.validation.MemberValidation;
import org.orcid.memberportal.service.member.web.rest.vm.AddConsortiumMember;
import org.orcid.memberportal.service.member.web.rest.vm.MemberContactUpdate;
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
    public void testGetMemberDetails() {
        Mockito.when(memberService.getCurrentMemberDetails()).thenReturn(getMemberDetails());
        ResponseEntity<MemberDetails> entity = memberResource.getMemberDetails();
        assertEquals(200, entity.getStatusCodeValue());

        MemberDetails memberDetails = entity.getBody();

        assertThat(memberDetails).isNotNull();
        assertThat(memberDetails.getName()).isEqualTo("test member details");
        assertThat(memberDetails.getPublicDisplayName()).isEqualTo("public display name");
        assertThat(memberDetails.getWebsite()).isEqualTo("https://website.com");
        assertThat(memberDetails.getMembershipStartDateString()).isEqualTo("2022-01-01");
        assertThat(memberDetails.getMembershipEndDateString()).isEqualTo("2027-01-01");
        assertThat(memberDetails.getPublicDisplayEmail()).isEqualTo("orcid@testmember.com");
        assertThat(memberDetails.getConsortiaLeadId()).isNull();
        assertThat(memberDetails.isConsortiaMember()).isFalse();
        assertThat(memberDetails.getPublicDisplayDescriptionHtml()).isEqualTo("<p>public display description</p>");
        assertThat(memberDetails.getMemberType()).isEqualTo("Research Institute");
        assertThat(memberDetails.getLogoUrl()).isEqualTo("some/url/for/a/logo");
        assertThat(memberDetails.getBillingCountry()).isEqualTo("Denmark");
        assertThat(memberDetails.getId()).isEqualTo("id");
    }

    @Test
    public void testUpdatePublicMemberDetails() {
        Mockito.when(memberService.updatePublicMemberDetails(Mockito.any(PublicMemberDetails.class))).thenReturn(Boolean.TRUE);
        PublicMemberDetails publicMemberDetails = getPublicMemberDetails();
        ResponseEntity<Boolean> response = memberResource.updatePublicMemberDetails(publicMemberDetails);
        assertEquals(200, response.getStatusCodeValue());
        Mockito.verify(memberService).updatePublicMemberDetails(Mockito.any(PublicMemberDetails.class));
    }

    @Test
    public void testUpdatePublicMemberDetailsWithEmptyName() {
        Mockito.when(memberService.updatePublicMemberDetails(Mockito.any(PublicMemberDetails.class))).thenReturn(Boolean.FALSE);
        PublicMemberDetails publicMemberDetails = getPublicMemberDetails();
        publicMemberDetails.setName("");
        ResponseEntity<Boolean> response = memberResource.updatePublicMemberDetails(publicMemberDetails);
        assertTrue(response.getStatusCode().is4xxClientError());
        Mockito.verify(memberService, Mockito.never()).updatePublicMemberDetails(Mockito.any(PublicMemberDetails.class));
    }

    @Test
    public void testGetMemberContacts() {
        Mockito.when(memberService.getCurrentMemberContacts()).thenReturn(getMemberContacts());
        ResponseEntity<MemberContacts> entity = memberResource.getMemberContacts();
        assertEquals(200, entity.getStatusCodeValue());

        MemberContacts memberContacts = entity.getBody();
        assertThat(memberContacts).isNotNull();
        assertThat(memberContacts.getTotalSize()).isEqualTo(2);
        assertThat(memberContacts.getRecords()).isNotNull();
        assertThat(memberContacts.getRecords().size()).isEqualTo(2);
        assertThat(memberContacts.getRecords().get(0).getName()).isEqualTo("contact 1");
        assertThat(memberContacts.getRecords().get(0).getTitle()).isEqualTo("Dr");
        assertThat(memberContacts.getRecords().get(0).getEmail()).isEqualTo("contact1@orcid.org");
        assertThat(memberContacts.getRecords().get(0).getRole()).isEqualTo("contact one role");
        assertThat(memberContacts.getRecords().get(0).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(memberContacts.getRecords().get(0).isVotingContact()).isEqualTo(false);
        assertThat(memberContacts.getRecords().get(1).getName()).isEqualTo("contact 2");
        assertThat(memberContacts.getRecords().get(1).getPhone()).isEqualTo("123456789");
        assertThat(memberContacts.getRecords().get(1).getEmail()).isEqualTo("contact2@orcid.org");
        assertThat(memberContacts.getRecords().get(1).getRole()).isEqualTo("contact two role");
        assertThat(memberContacts.getRecords().get(1).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(memberContacts.getRecords().get(1).isVotingContact()).isEqualTo(true);
    }

    @Test
    public void testGetMemberOrgIds() {
        Mockito.when(memberService.getCurrentMemberOrgIds()).thenReturn(getMemberOrgIds());
        ResponseEntity<MemberOrgIds> entity = memberResource.getMemberOrgIds();
        assertEquals(200, entity.getStatusCodeValue());

        MemberOrgIds memberOrgIds = memberService.getCurrentMemberOrgIds();
        assertThat(memberOrgIds).isNotNull();
        assertThat(memberOrgIds.getTotalSize()).isEqualTo(2);
        assertThat(memberOrgIds.getRecords()).isNotNull();
        assertThat(memberOrgIds.getRecords().size()).isEqualTo(2);
        assertThat(memberOrgIds.getRecords().get(0).getType()).isEqualTo("Ringgold ID");
        assertThat(memberOrgIds.getRecords().get(0).getValue()).isEqualTo("9988776655");
        assertThat(memberOrgIds.getRecords().get(1).getType()).isEqualTo("GRID");
        assertThat(memberOrgIds.getRecords().get(1).getValue()).isEqualTo("grid.238252");
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

    @Test
    public void testUpdateMemberDefaultLanguage() {
        ResponseEntity<Void> response = memberResource.updateMemberDefaultLanguage("salesforceId", "en");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Mockito.verify(memberService).updateMemberDefaultLanguage(Mockito.eq("salesforceId"), Mockito.eq("en"));
    }

    @Test
    public void testProcessMemberContactUpdate() {
        Mockito.doNothing().when(memberService).processMemberContact(Mockito.any(MemberContactUpdate.class));
        memberResource.processMemberContactUpdate(new MemberContactUpdate());
        Mockito.verify(memberService).processMemberContact(Mockito.any(MemberContactUpdate.class));
    }

    @Test
    public void testRequestNewConsortiumMember() {
        Mockito.doNothing().when(memberService).requestNewConsortiumMember(Mockito.any(AddConsortiumMember.class));
        memberResource.requestNewConsortiumMember(new AddConsortiumMember());
        Mockito.verify(memberService).requestNewConsortiumMember(Mockito.any(AddConsortiumMember.class));
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

    private MemberDetails getMemberDetails() {
        MemberDetails memberDetails = new MemberDetails();
        memberDetails.setBillingCountry("Denmark");
        memberDetails.setConsortiaLeadId(null);
        memberDetails.setConsortiaMember(false);
        memberDetails.setId("id");
        memberDetails.setLogoUrl("some/url/for/a/logo");
        memberDetails.setMemberType("Research Institute");
        memberDetails.setName("test member details");
        memberDetails.setPublicDisplayDescriptionHtml("<p>public display description</p>");
        memberDetails.setPublicDisplayEmail("orcid@testmember.com");
        memberDetails.setPublicDisplayName("public display name");
        memberDetails.setMembershipStartDateString("2022-01-01");
        memberDetails.setMembershipEndDateString("2027-01-01");
        memberDetails.setWebsite("https://website.com");
        return memberDetails;
    }

    private PublicMemberDetails getPublicMemberDetails() {
        PublicMemberDetails publicMemberDetails = new PublicMemberDetails();
        publicMemberDetails.setName("test member details");
        publicMemberDetails.setWebsite("https://website.com");
        publicMemberDetails.setDescription("test");
        publicMemberDetails.setEmail("email@orcid.org");
        return publicMemberDetails;
    }

    private MemberContacts getMemberContacts() {
        MemberContacts memberContacts = new MemberContacts();

        MemberContact contact1 = new MemberContact();
        contact1.setName("contact 1");
        contact1.setTitle("Dr");
        contact1.setEmail("contact1@orcid.org");
        contact1.setRole("contact one role");
        contact1.setSalesforceId("salesforce-id");
        contact1.setVotingContact(false);

        MemberContact contact2 = new MemberContact();
        contact2.setName("contact 2");
        contact2.setPhone("123456789");
        contact2.setEmail("contact2@orcid.org");
        contact2.setRole("contact two role");
        contact2.setSalesforceId("salesforce-id");
        contact2.setVotingContact(true);

        memberContacts.setTotalSize(2);
        memberContacts.setRecords(Arrays.asList(contact1, contact2));

        return memberContacts;
    }

    private MemberOrgIds getMemberOrgIds() {
        MemberOrgId orgId1 = new MemberOrgId();
        orgId1.setType("Ringgold ID");
        orgId1.setValue("9988776655");

        MemberOrgId orgId2 = new MemberOrgId();
        orgId2.setType("GRID");
        orgId2.setValue("grid.238252");

        MemberOrgIds memberOrgIds = new MemberOrgIds();
        memberOrgIds.setTotalSize(2);
        memberOrgIds.setRecords(Arrays.asList(orgId1, orgId2));

        return memberOrgIds;
    }

}
