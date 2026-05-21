package org.orcid.mp.member.rest;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.UnauthorizedMemberAccessException;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.MemberContactUpdateResponse;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.security.MockSecurityContext;
import org.orcid.mp.member.service.MemberService;
import org.orcid.mp.member.service.SalesforceService;
import org.orcid.mp.member.service.UserService;
import org.orcid.mp.member.validation.MemberValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class MemberResourceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private SalesforceService salesforceService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MemberResource memberResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Member member = new Member();
        member.setId("memberId");
        member.setSalesforceId("salesforceId");
        when(memberService.getMember(eq("memberId"))).thenReturn(Optional.of(member));

        SecurityContextHolder.setContext(new MockSecurityContext("me"));
        when(userService.getLoggedInUser()).thenReturn(getUser());
    }

    @Test
    public void testValidateMember() throws URISyntaxException, JSONException {
        when(memberService.validateMember(Mockito.any(Member.class))).thenReturn(getMemberValidation());
        ResponseEntity<MemberValidation> validationResponse = memberResource.validateMember(new Member());

        // always 200, even if invalid member; the request to validate is valid
        assertEquals(200, validationResponse.getStatusCodeValue());
        assertTrue(validationResponse.getBody().isValid());
        assertEquals(1, validationResponse.getBody().getErrors().size());
    }

    @Test
    public void testGetMemberDetails() throws UnauthorizedMemberAccessException {
        when(salesforceService.getMemberDetails(eq("salesforceId"))).thenReturn(getMemberDetails());
        ResponseEntity<MemberDetails> entity = memberResource.getMemberDetails("memberId");
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
    public void testGetMemberDetails_unauthorised() throws UnauthorizedMemberAccessException {
        when(userService.getLoggedInUser()).thenReturn(getSomeOtherUser());
        when(memberService.getMember(eq("memberId"))).thenReturn(Optional.of(getSomeOtherMember()));
        ResponseEntity<MemberDetails> entity = memberResource.getMemberDetails("memberId");
        assertEquals(401, entity.getStatusCodeValue());
    }

    @Test
    public void testUpdatePublicMemberDetails() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(salesforceService).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));
        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        ResponseEntity<Boolean> response = memberResource.updatePublicMemberDetails(memberUpdateData, "memberId");
        assertEquals(200, response.getStatusCodeValue());
        Mockito.verify(salesforceService).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));
    }

    @Test
    public void testUpdatePublicMemberDetailsWithBillingAddress() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(salesforceService).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));
        MemberUpdateData memberUpdateData = getPublicMemberDetailsWithBillingAddress();
        ResponseEntity<Boolean> response = memberResource.updatePublicMemberDetails(memberUpdateData, "memberId");
        assertEquals(200, response.getStatusCodeValue());
        Mockito.verify(salesforceService).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));
    }

    @Test
    public void testUpdatePublicMemberDetailsWithBillingAddressAndNullCountry() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(salesforceService).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));
        MemberUpdateData memberUpdateData = getPublicMemberDetailsWithBillingAddressAndNullCountry();
        ResponseEntity<Boolean> response = memberResource.updatePublicMemberDetails(memberUpdateData, "memberId");
        assertEquals(400, response.getStatusCodeValue());
        Mockito.verify(salesforceService, Mockito.never()).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));
    }

    @Test
    public void testUpdatePublicMemberDetails_unauthorised() throws UnauthorizedMemberAccessException {
        when(userService.getLoggedInUser()).thenReturn(getSomeOtherUser());
        when(memberService.getMember(eq("some-other-salesforceId"))).thenReturn(Optional.of(getSomeOtherMember()));
        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        ResponseEntity<Boolean> response = memberResource.updatePublicMemberDetails(memberUpdateData, "memberId");
        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    public void testUpdatePublicMemberDetailsWithEmptyName() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(salesforceService).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));
        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        memberUpdateData.setPublicName("");
        ResponseEntity<Boolean> response = memberResource.updatePublicMemberDetails(memberUpdateData, "memberId");
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    public void testGetMemberContacts() throws UnauthorizedMemberAccessException {
        when(salesforceService.getMemberContacts(eq("salesforceId"))).thenReturn(getMemberContacts());
        ResponseEntity<MemberContacts> entity = memberResource.getMemberContacts("memberId");
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
    public void testGetMemberContacts_unauthorised() throws UnauthorizedMemberAccessException {
        when(userService.getLoggedInUser()).thenReturn(getSomeOtherUser());
        when(memberService.getMember(eq("some-other-salesforceId"))).thenReturn(Optional.of(getSomeOtherMember()));
        ResponseEntity<MemberContacts> entity = memberResource.getMemberContacts("salesforceId");
        assertEquals(401, entity.getStatusCodeValue());
    }

    @Test
    public void testGetMemberOrgIds() throws UnauthorizedMemberAccessException {
        when(salesforceService.getMemberOrgIds(eq("salesforceId"))).thenReturn(getMemberOrgIds());
        ResponseEntity<MemberOrgIds> entity = memberResource.getMemberOrgIds("memberId");
        assertEquals(200, entity.getStatusCodeValue());

        MemberOrgIds memberOrgIds = entity.getBody();
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
    public void testGetMemberOrgIds_unauthorised() {
        when(userService.getLoggedInUser()).thenReturn(getSomeOtherUser()); // doesn't match below sf id
        ResponseEntity<MemberOrgIds> entity = memberResource.getMemberOrgIds("salesforceId");
        assertEquals(401, entity.getStatusCodeValue());

        when(userService.getLoggedInUser()).thenReturn(getSomeOtherUser());
        when(memberService.getMember(eq("some-other-salesforceId"))).thenReturn(Optional.of(getSomeOtherMember()));
        entity = memberResource.getMemberOrgIds("salesforceId");
        assertEquals(401, entity.getStatusCodeValue());
    }

    @Test
    public void testGetAllMembers() {
        when(memberService.getMembers(Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(getMember(), getMember(), getMember(), getMember())));
        when(memberService.getMembers(Mockito.any(Pageable.class), Mockito.anyString())).thenReturn(new PageImpl<>(Arrays.asList(getMember(), getMember())));

        ResponseEntity<Page<Member>> response = memberResource.getAllMembers("", Mockito.mock(Pageable.class));
        assertNotNull(response);
        Page<Member> members = response.getBody();
        assertEquals(4, members.getTotalElements());
        Mockito.verify(memberService, Mockito.times(1)).getMembers(Mockito.any(Pageable.class));

        response = memberResource.getAllMembers("some-filter", Mockito.mock(Pageable.class));
        assertNotNull(response);
        members = response.getBody();
        assertEquals(2, members.getTotalElements());
        Mockito.verify(memberService, Mockito.times(1)).getMembers(Mockito.any(Pageable.class), Mockito.anyString());
    }

    @Test
    public void testUpdateMemberDefaultLanguage() throws UnauthorizedMemberAccessException {
        ResponseEntity<Void> response = memberResource.updateMemberDefaultLanguage("memberId", "en");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Mockito.verify(memberService).updateMemberDefaultLanguage(eq("memberId"), eq("en"));
    }

    @Test
    public void testProcessMemberContactUpdate() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(salesforceService).processMemberContact(Mockito.any(MemberContactUpdate.class), eq("salesforceId"));
        memberResource.processMemberContactUpdate(new MemberContactUpdate(), "memberId");
        Mockito.verify(salesforceService).processMemberContact(Mockito.any(MemberContactUpdate.class), eq("salesforceId"));
    }

    @Test
    public void testProcessMemberContactUpdate_unauthorised() throws UnauthorizedMemberAccessException {
        when(userService.getLoggedInUser()).thenReturn(getSomeOtherUser());
        when(memberService.getMember(eq("some-other-salesforceId"))).thenReturn(Optional.of(getSomeOtherMember()));
        ResponseEntity<MemberContactUpdateResponse> response = memberResource.processMemberContactUpdate(new MemberContactUpdate(), "salesforceId");
        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    public void testRequestNewConsortiumMember() {
        Mockito.doNothing().when(salesforceService).requestNewConsortiumMember(Mockito.any(AddConsortiumMember.class));
        memberResource.requestNewConsortiumMember(new AddConsortiumMember());
        Mockito.verify(salesforceService).requestNewConsortiumMember(Mockito.any(AddConsortiumMember.class));
    }

    @Test
    public void testRequestRemoveConsortiumMember() {
        Mockito.doNothing().when(salesforceService).requestRemoveConsortiumMember(Mockito.any(RemoveConsortiumMember.class));
        memberResource.requestRemoveConsortiumMember(new RemoveConsortiumMember());
        Mockito.verify(salesforceService).requestRemoveConsortiumMember(Mockito.any(RemoveConsortiumMember.class));
    }

    @Test
    public void testGetSalesforceCountries() {
        when(salesforceService.getSalesforceCountries()).thenReturn(getSalesforceCountries());
        memberResource.getSalesforceCountries();
        Mockito.verify(salesforceService).getSalesforceCountries();
    }

    private List<Country> getSalesforceCountries() {
        State state1 = new State();
        state1.setName("state1");
        state1.setCode("s1");

        State state2 = new State();
        state2.setName("state2");
        state2.setCode("s2");

        Country country = new Country();
        country.setName("country1");
        country.setCode("c1");
        country.setStates(Arrays.asList(state1, state2));

        return Arrays.asList(country);
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
        member.setSalesforceId("salesforceId");
        member.setParentSalesforceId("parentSalesforceId");
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

    private MemberUpdateData getPublicMemberDetails() {
        MemberUpdateData memberUpdateData = new MemberUpdateData();
        memberUpdateData.setPublicName("test member details");
        memberUpdateData.setWebsite("https://website.com");
        memberUpdateData.setOrgName("orgName");
        memberUpdateData.setDescription("test");
        memberUpdateData.setEmail("email@orcid.org");
        return memberUpdateData;
    }

    private MemberUpdateData getPublicMemberDetailsWithBillingAddress() {
        MemberUpdateData memberUpdateData = new MemberUpdateData();
        memberUpdateData.setPublicName("test member details");
        memberUpdateData.setWebsite("https://website.com");
        memberUpdateData.setOrgName("orgName");
        memberUpdateData.setDescription("test");
        memberUpdateData.setEmail("email@orcid.org");

        BillingAddress billingAddress = new BillingAddress();
        billingAddress.setCity("new york");
        billingAddress.setCountry("USA");
        memberUpdateData.setBillingAddress(billingAddress);
        return memberUpdateData;
    }

    private MemberUpdateData getPublicMemberDetailsWithBillingAddressAndNullCountry() {
        MemberUpdateData memberUpdateData = new MemberUpdateData();
        memberUpdateData.setPublicName("test member details");
        memberUpdateData.setWebsite("https://website.com");
        memberUpdateData.setOrgName("orgName");
        memberUpdateData.setDescription("test");
        memberUpdateData.setEmail("email@orcid.org");

        BillingAddress billingAddress = new BillingAddress();
        billingAddress.setCity("new york");
        memberUpdateData.setBillingAddress(billingAddress);
        return memberUpdateData;
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

    private User getUser() {
        User user = new User();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        user.setMemberId("memberId");
        user.setMemberName("member");
        return user;
    }

    private User getParentUser() {
        User user = new User();
        user.setEmail("parent-user@orcid.org");
        user.setLangKey("en");
        user.setMemberId("parentMemberId");
        user.setMemberName("member");
        return user;
    }

    private User getSomeOtherUser() {
        User user = new User();
        user.setEmail("some-other-user@orcid.org");
        user.setLangKey("en");
        user.setMemberId("some-other-memberId");
        user.setMemberName("member");
        return user;
    }

    private Member getSomeOtherMember() {
        Member member = new Member();
        member.setSalesforceId("some-other-salesforceId");
        return member;
    }

}