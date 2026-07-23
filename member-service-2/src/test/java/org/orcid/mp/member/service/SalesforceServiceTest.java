package org.orcid.mp.member.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.orcid.mp.member.client.SalesforceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.UnauthorizedMemberAccessException;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.security.MockSecurityContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SalesforceServiceTest {

    @Mock
    private SalesforceClient salesforceClient;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @Captor
    private ArgumentCaptor<MemberUpdateData> publicMemberDetailsCaptor;

    @Mock
    private MemberService memberService;

    @Captor
    private ArgumentCaptor<Member> salesforceUpdateCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private SalesforceService salesforceService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        salesforceService = new SalesforceService(memberService, userService, mailService, salesforceClient, objectMapper);
        SecurityContextHolder.setContext(new MockSecurityContext("me"));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
    }

    @Test
    void testGetMemberDetails() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(salesforceClient.getMemberDetails(Mockito.eq("salesforceId"))).thenReturn(getFileContent("src/test/resources/salesforce/member.json"));

        MemberDetails memberDetails = salesforceService.getMemberDetails("salesforceId");
        assertThat(memberDetails).isNotNull();
        assertThat(memberDetails.getName()).isEqualTo("AAtest Lead");
        assertThat(memberDetails.getPublicDisplayName()).isEqualTo("AAtest Lead");
        assertThat(memberDetails.getMembershipStartDateString()).isEqualTo("2022-07-01");
        assertThat(memberDetails.getMembershipEndDateString()).isEqualTo("2022-12-31");
        assertThat(memberDetails.getConsortiaLeadId()).isEqualTo("a032i000000l6scAAAXXX");
        assertThat(memberDetails.isConsortiaMember()).isTrue();
        assertThat(memberDetails.getBillingAddress().getCountry()).isEqualTo("Andorra");
        assertThat(memberDetails.getId()).isEqualTo("a032i000000l6scAAA");
    }

    @Test
    void testGetMemberContacts() throws IOException {
        when(salesforceClient.getMemberContacts(Mockito.eq("salesforceId"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactRoles.json"));
        when(salesforceClient.getMemberContactData(eq("1"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName1.json"));
        when(salesforceClient.getMemberContactData(eq("2"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName2.json"));
        when(salesforceClient.getMemberContactData(eq("3"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName3.json"));
        when(salesforceClient.getMemberContactData(eq("4"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName4.json"));

        MemberContacts memberContacts = salesforceService.getMemberContacts("salesforceId");
        assertThat(memberContacts).isNotNull();
        assertThat(memberContacts.getTotalSize()).isEqualTo(4);
        assertThat(memberContacts.getRecords()).isNotNull();
        assertThat(memberContacts.getRecords().size()).isEqualTo(4);
        assertThat(memberContacts.getRecords().get(0).getName()).isEqualTo("Contact 1");
        assertThat(memberContacts.getRecords().get(0).getTitle()).isEqualTo("Dr");
        assertThat(memberContacts.getRecords().get(0).getPhone()).isEqualTo("987654");
        assertThat(memberContacts.getRecords().get(0).getEmail()).isEqualTo("contact.1@orcid.org");
        assertThat(memberContacts.getRecords().get(0).getRole()).isEqualTo("Main relationship contact (OFFICIAL)");
        assertThat(memberContacts.getRecords().get(0).getSalesforceId()).isEqualTo("some-id");
        assertThat(memberContacts.getRecords().get(0).isVotingContact()).isEqualTo(false);
        assertThat(memberContacts.getRecords().get(1).getName()).isEqualTo("Contact 2");
        assertThat(memberContacts.getRecords().get(1).getTitle()).isNull();
        assertThat(memberContacts.getRecords().get(1).getPhone()).isEqualTo("123456789");
        assertThat(memberContacts.getRecords().get(1).getEmail()).isEqualTo("contact.2@orcid.org");
        assertThat(memberContacts.getRecords().get(1).getRole()).isEqualTo("Technical contact");
        assertThat(memberContacts.getRecords().get(1).getSalesforceId()).isEqualTo("some-id");
        assertThat(memberContacts.getRecords().get(1).isVotingContact()).isEqualTo(false);
        assertThat(memberContacts.getRecords().get(2).getName()).isEqualTo("Contact 3");
        assertThat(memberContacts.getRecords().get(2).getTitle()).isNull();
        assertThat(memberContacts.getRecords().get(2).getPhone()).isNull();
        assertThat(memberContacts.getRecords().get(2).getEmail()).isEqualTo("contact.3@orcid.org");
        assertThat(memberContacts.getRecords().get(2).getRole()).isEqualTo("Product Contact");
        assertThat(memberContacts.getRecords().get(2).getSalesforceId()).isEqualTo("some-id");
        assertThat(memberContacts.getRecords().get(2).isVotingContact()).isEqualTo(false);
        assertThat(memberContacts.getRecords().get(3).getName()).isEqualTo("Contact 4");
        assertThat(memberContacts.getRecords().get(3).getTitle()).isEqualTo("Mrs");
        assertThat(memberContacts.getRecords().get(3).getPhone()).isNull();
        assertThat(memberContacts.getRecords().get(3).getEmail()).isEqualTo("contact.4@orcid.org");
        assertThat(memberContacts.getRecords().get(3).getRole()).isEqualTo("Agreement signatory (OFFICIAL)");
        assertThat(memberContacts.getRecords().get(3).getSalesforceId()).isEqualTo("some-id");
        assertThat(memberContacts.getRecords().get(3).isVotingContact()).isEqualTo(true);

    }

    @Test
    void testGetMemberOrgIds() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(salesforceClient.getMemberOrgIds(Mockito.eq("salesforceId"))).thenReturn(getFileContent("src/test/resources/salesforce/memberOrgIds.json"));

        MemberOrgIds memberOrgIds = salesforceService.getMemberOrgIds("salesforceId");
        assertThat(memberOrgIds).isNotNull();
        assertThat(memberOrgIds.getTotalSize()).isEqualTo(4);
        assertThat(memberOrgIds.getRecords()).isNotNull();
        assertThat(memberOrgIds.getRecords().size()).isEqualTo(4);
        assertThat(memberOrgIds.getRecords().get(0).getType()).isEqualTo("Ringgold ID");
        assertThat(memberOrgIds.getRecords().get(0).getValue()).isEqualTo("9988776655");
        assertThat(memberOrgIds.getRecords().get(1).getType()).isEqualTo("GRID");
        assertThat(memberOrgIds.getRecords().get(1).getValue()).isEqualTo("grid.238252");
        assertThat(memberOrgIds.getRecords().get(2).getType()).isEqualTo("FundRef ID");
        assertThat(memberOrgIds.getRecords().get(2).getValue()).isEqualTo("aldskj");
        assertThat(memberOrgIds.getRecords().get(3).getType()).isEqualTo("ROR");
        assertThat(memberOrgIds.getRecords().get(3).getValue()).isEqualTo("rorror");
    }

    @Test
    void testGetMemberOrgIds_forConsortiumMemberByConsortiumLead() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());
        Mockito.when(salesforceClient.getMemberOrgIds(Mockito.eq("salesforceId"))).thenReturn(objectMapper.writeValueAsString(getMemberOrgIds()));

        MemberOrgIds memberOrgIds = salesforceService.getMemberOrgIds("salesforceId");
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
    void testGetConsortiumLeadDetails() throws IOException {
        Mockito.when(salesforceClient.getMemberDetails(Mockito.eq("salesforceId"))).thenReturn(getFileContent("src/test/resources/salesforce/member.json"));
        Mockito.when(salesforceClient.getConsortium(Mockito.eq("salesforceId"))).thenReturn(getFileContent("src/test/resources/salesforce/consortium.json"));

        ConsortiumLeadDetails details = salesforceService.getConsortiumLeadDetails("salesforceId");
        assertThat(details).isNotNull();
        assertThat(details.getName()).isEqualTo("AAtest Lead");
        assertThat(details.getPublicDisplayName()).isEqualTo("AAtest Lead");
        assertThat(details.getMembershipStartDateString()).isEqualTo("2022-07-01");
        assertThat(details.getMembershipEndDateString()).isEqualTo("2022-12-31");
        assertThat(details.getConsortiaLeadId()).isEqualTo("a032i000000l6scAAAXXX");
        assertThat(details.isConsortiaMember()).isTrue();
        assertThat(details.getBillingAddress().getCountry()).isEqualTo("Andorra");
        assertThat(details.getId()).isEqualTo("a032i000000l6scAAA");
        assertThat(details.getBillingAddress().getCountry()).isEqualTo("Andorra");
        assertThat(details.getConsortiumMembers()).isNotNull();
        assertThat(details.getConsortiumMembers().size()).isEqualTo(1);
        assertThat(details.getConsortiumMembers().getFirst().getSalesforceId()).isEqualTo("some-consortium-member-id");
    }

    @Test
    void testUpdatePublicMemberDetails() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.doNothing().when(salesforceClient).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));

        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        salesforceService.updatePublicMemberDetails(memberUpdateData);

        Mockito.verify(salesforceClient).updatePublicMemberDetails(publicMemberDetailsCaptor.capture());
        MemberUpdateData details = publicMemberDetailsCaptor.getValue();
        assertThat(details).isNotNull();
        assertThat(details.getSalesforceId()).isEqualTo("salesforceId");
        assertThat(details.getPublicName()).isEqualTo(memberUpdateData.getPublicName());
        assertThat(details.getDescription()).isEqualTo(memberUpdateData.getDescription());
        assertThat(details.getWebsite()).isEqualTo(memberUpdateData.getWebsite());
        assertThat(details.getEmail()).isEqualTo(memberUpdateData.getEmail());
        assertThat(details.getOrgName()).isEqualTo(memberUpdateData.getOrgName());
    }

    @Test
    void testUpdatePublicMemberDetails_forConsortiumMemberByConsortiumLead() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());
        Mockito.doNothing().when(salesforceClient).updatePublicMemberDetails(Mockito.any(MemberUpdateData.class));

        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        salesforceService.updatePublicMemberDetails(memberUpdateData);

        Mockito.verify(salesforceClient).updatePublicMemberDetails(publicMemberDetailsCaptor.capture());
        MemberUpdateData details = publicMemberDetailsCaptor.getValue();
        assertThat(details).isNotNull();
        assertThat(details.getSalesforceId()).isEqualTo("salesforceId");
        assertThat(details.getPublicName()).isEqualTo(memberUpdateData.getPublicName());
        assertThat(details.getDescription()).isEqualTo(memberUpdateData.getDescription());
        assertThat(details.getWebsite()).isEqualTo(memberUpdateData.getWebsite());
        assertThat(details.getEmail()).isEqualTo(memberUpdateData.getEmail());
    }

    @Test
    void testProcessMemberContact_update() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");
        update.setContactNewEmail("a.new.contact@email.com");

        salesforceService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService).getLoggedInUser();
    }

    @Test
    void testAddConsortiumMember() {
        Mockito.doNothing().when(mailService).sendAddConsortiumMemberEmail(Mockito.any(AddConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberService.getMember(Mockito.eq("memberId"))).thenReturn(Optional.of(getConsortiumLeadMember()));

        AddConsortiumMember addConsortiumMember = new AddConsortiumMember();
        addConsortiumMember.setOrgName("new org name");

        salesforceService.requestNewConsortiumMember(addConsortiumMember);

        Mockito.verify(mailService).sendAddConsortiumMemberEmail(Mockito.any(AddConsortiumMember.class));
        Mockito.verify(userService).getLoggedInUser();
        Mockito.verify(memberService).getMember(Mockito.eq("memberId"));
    }

    @Test
    void testRemoveConsortiumMember_nonCL() {
        Mockito.doNothing().when(mailService).sendRemoveConsortiumMemberEmail(Mockito.any(RemoveConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        RemoveConsortiumMember removeConsortiumMember = new RemoveConsortiumMember();
        removeConsortiumMember.setOrgName("old org name");

        Assertions.assertThrows(RuntimeException.class, () -> {
            salesforceService.requestRemoveConsortiumMember(removeConsortiumMember);
        });
    }

    @Test
    void testRemoveConsortiumMember() {
        Mockito.doNothing().when(mailService).sendRemoveConsortiumMemberEmail(Mockito.any(RemoveConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberService.getMember(Mockito.eq("memberId"))).thenReturn(Optional.of(getConsortiumLeadMember()));

        RemoveConsortiumMember removeConsortiumMember = new RemoveConsortiumMember();
        removeConsortiumMember.setOrgName("old org name");

        salesforceService.requestRemoveConsortiumMember(removeConsortiumMember);

        Mockito.verify(mailService).sendRemoveConsortiumMemberEmail(Mockito.any(RemoveConsortiumMember.class));
        Mockito.verify(userService).getLoggedInUser();
        Mockito.verify(memberService).getMember(Mockito.eq("memberId"));
    }

    @Test
    void testAddConsortiumMember_nonCL() {
        Mockito.doNothing().when(mailService).sendAddConsortiumMemberEmail(Mockito.any(AddConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        AddConsortiumMember addConsortiumMember = new AddConsortiumMember();
        addConsortiumMember.setOrgName("new org name");

        Assertions.assertThrows(RuntimeException.class, () -> {
            salesforceService.requestNewConsortiumMember(addConsortiumMember);
        });
    }

    @Test
    void testProcessMemberContact_add() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactNewEmail("a.contact@email.com");

        salesforceService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService).getLoggedInUser();
    }

    @Test
    void testProcessMemberContact_remove() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");

        salesforceService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService).getLoggedInUser();
    }

    @Test
    void testGetSalesforceCountries() throws Exception {
        when(salesforceClient.getMetadata()).thenReturn(getTestMetadata());
        List<Country> countries = salesforceService.getSalesforceCountries();
        assertNotNull(countries);
        assertEquals(240, countries.size());

        Country unitedStates = countries.get(228);
        assertNotNull(unitedStates.getStates());
        assertEquals(58, unitedStates.getStates().size());
        verify(salesforceClient).getMetadata();

        Country ireland = countries.get(103);
        assertNotNull(ireland.getStates());
        assertEquals(26, ireland.getStates().size());
    }

    @Test
    void testSyncMembers_updates() throws IOException {
        when(salesforceClient.getMembers()).thenReturn(getFileContent("src/test/resources/salesforce/members.json"));
        when(salesforceClient.getConsortium(anyString())).thenReturn("{}");
        doAnswer(invocation -> invocation.getArgument(0)).when(memberService).updateMember(Mockito.any(), anyString());
        when(salesforceClient.getMemberContacts(anyString())).thenReturn("{}");
        when(memberService.getMember(Mockito.eq("0011000001XYZ01"))).thenReturn(Optional.of(getMember("0011000001XYZ01", false)));
        when(memberService.getMember(Mockito.eq("0011000002XYZ02"))).thenReturn(Optional.of(getMember("0011000002XYZ02", true)));
        when(memberService.getMember(Mockito.eq("0011000003XYZ03"))).thenReturn(Optional.of(getMember("0011000003XYZ03", true)));
        when(memberService.getMember(Mockito.eq("0011000004XYZ04"))).thenReturn(Optional.of(getMember("0011000004XYZ04", true)));
        when(memberService.getMember(Mockito.eq("0011000005XYZ05"))).thenReturn(Optional.of(getMember("0011000005XYZ05", false)));

        salesforceService.syncMembers();

        verify(memberService, times(5)).updateMember(salesforceUpdateCaptor.capture(), anyString());

        List<Member> updatedMembers = salesforceUpdateCaptor.getAllValues();
        assertThat(updatedMembers.get(0)).isNotNull();
        assertThat(updatedMembers.get(0).getSalesforceId()).isEqualTo("0011000001XYZ01");
        assertThat(updatedMembers.get(0).getClientName()).isEqualTo("Global Research University");
        assertThat(updatedMembers.get(0).isActive()).isTrue();
        assertThat(updatedMembers.get(0).getActivatedDate()).isNotNull();
        assertThat(updatedMembers.get(0).getDeactivatedDate()).isNull();
        assertThat(updatedMembers.get(1)).isNotNull();
        assertThat(updatedMembers.get(1).getSalesforceId()).isEqualTo("0011000002XYZ02");
        assertThat(updatedMembers.get(1).getClientName()).isEqualTo("Consortium Sub-Member A");
        assertThat(updatedMembers.get(1).isActive()).isTrue();
        assertThat(updatedMembers.get(1).getActivatedDate()).isNull(); // hasn't just been activated
        assertThat(updatedMembers.get(1).getDeactivatedDate()).isNull();
        assertThat(updatedMembers.get(2)).isNotNull();
        assertThat(updatedMembers.get(2).getSalesforceId()).isEqualTo("0011000003XYZ03");
        assertThat(updatedMembers.get(2).getClientName()).isEqualTo("Consortium Sub-Member B");
        assertThat(updatedMembers.get(2).isActive()).isTrue();
        assertThat(updatedMembers.get(2).getActivatedDate()).isNull();
        assertThat(updatedMembers.get(2).getDeactivatedDate()).isNull();
        assertThat(updatedMembers.get(3)).isNotNull();
        assertThat(updatedMembers.get(3).getSalesforceId()).isEqualTo("0011000004XYZ04");
        assertThat(updatedMembers.get(3).getClientName()).isEqualTo("Legacy Research Lab");
        assertThat(updatedMembers.get(3).isActive()).isFalse();
        assertThat(updatedMembers.get(3).getDeactivatedDate()).isNotNull();
        assertThat(updatedMembers.get(3).getActivatedDate()).isNull();
        assertThat(updatedMembers.get(4)).isNotNull();
        assertThat(updatedMembers.get(4).getSalesforceId()).isEqualTo("0011000005XYZ05");
        assertThat(updatedMembers.get(4).getClientName()).isEqualTo("New Horizon Publisher");
        assertThat(updatedMembers.get(4).isActive()).isTrue();
        assertThat(updatedMembers.get(4).getActivatedDate()).isNotNull();
        assertThat(updatedMembers.get(4).getDeactivatedDate()).isNull();
    }

    @Test
    void testSyncMembers_updateMemberToCL() throws IOException {
        when(salesforceClient.getMembers()).thenReturn(getFileContent("src/test/resources/salesforce/members.json"));
        when(salesforceClient.getConsortium(eq("0011000001XYZ01"))).thenReturn(getFileContent("src/test/resources/salesforce/consortium.json"));
        when(memberService.getMember(eq("0011000001XYZ01"))).thenReturn(Optional.of(getMember("0011000001XYZ01", false)));
        when(salesforceClient.getMemberDetails(eq("0011000001XYZ01"))).thenReturn(getFileContent("src/test/resources/salesforce/consortiumLead.json"));

        salesforceService.syncMembers();

        verify(memberService, Mockito.times(2)).updateMember(salesforceUpdateCaptor.capture(), anyString());
        verify(memberService).addParent(eq("some-consortium-member-id"), eq("0011000001XYZ01"), anyString());

        List<Member> updatedMembers = salesforceUpdateCaptor.getAllValues();
        Member updatedMember = updatedMembers.get(1); // get last updated
        assertThat(updatedMember).isNotNull();
        assertThat(updatedMember.getSalesforceId()).isEqualTo("0011000001XYZ01");
        assertThat(updatedMember.getIsConsortiumLead()).isTrue();
        assertThat(updatedMember.getLastModifiedDate()).isNotNull();
        assertThat(updatedMember.getLastModifiedBy()).isEqualTo(SalesforceService.SALESFORCE_SYNC_USERNAME);
    }

    @Test
    void testSyncMembers_updateCLToMember() throws IOException {
        Member cl = getMember("0011000001XYZ01", true);
        cl.setIsConsortiumLead(true);

        when(salesforceClient.getMembers()).thenReturn(getFileContent("src/test/resources/salesforce/members.json"));
        when(salesforceClient.getConsortium(eq("0011000001XYZ01"))).thenReturn("{}");
        when(memberService.getMember(eq("0011000001XYZ01"))).thenReturn(Optional.of(cl));
        when(salesforceClient.getMemberDetails(eq("0011000001XYZ01"))).thenReturn(getFileContent("src/test/resources/salesforce/consortiumLead.json"));

        salesforceService.syncMembers();

        verify(memberService).updateMember(salesforceUpdateCaptor.capture(), anyString());

        Member updatedMember = salesforceUpdateCaptor.getValue();
        assertThat(updatedMember).isNotNull();
        assertThat(updatedMember.getSalesforceId()).isEqualTo("0011000001XYZ01");
        assertThat(updatedMember.getIsConsortiumLead()).isFalse();
    }

    @Test
    void testSyncMembers_updateWithMainContact() throws IOException {
        when(salesforceClient.getMembers()).thenReturn(getFileContent("src/test/resources/salesforce/members.json"));
        when(salesforceClient.getConsortium(anyString())).thenReturn("{}");
        doAnswer(invocation -> invocation.getArgument(0)).when(memberService).createMember(Mockito.any(), anyString());

        when(memberService.getMember(eq("0011000001XYZ01"))).thenReturn(Optional.empty()).thenReturn(Optional.of(getMember("0011000001XYZ01", false)));
        when(userService.getUsersByMemberId(anyString())).thenReturn(new ArrayList<>());
        when(salesforceClient.getMemberContacts(Mockito.eq("0011000001XYZ01"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactRoles.json"));
        when(salesforceClient.getMemberContactData(eq("1"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName1.json"));
        when(salesforceClient.getMemberContactData(eq("2"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName2.json"));
        when(salesforceClient.getMemberContactData(eq("3"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName3.json"));
        when(salesforceClient.getMemberContactData(eq("4"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName4.json"));

        salesforceService.syncMembers();

        verify(userService).createMainContactUser(userCaptor.capture());

        User captured = userCaptor.getValue();
        assertThat(captured.getCreatedBy()).isEqualTo(SalesforceService.SALESFORCE_SYNC_USERNAME);
        assertThat(captured.getMainContact()).isTrue();
    }

    @Test
    void testSyncMembers_creates() throws IOException {
        when(salesforceClient.getMembers()).thenReturn(getFileContent("src/test/resources/salesforce/members.json"));
        when(salesforceClient.getConsortium(anyString())).thenReturn("{}");
        doAnswer(invocation -> invocation.getArgument(0)).when(memberService).createMember(Mockito.any(), anyString());

        salesforceService.syncMembers();

        verify(memberService, times(5)).createMember(salesforceUpdateCaptor.capture(), anyString());

        List<Member> updatedMembers = salesforceUpdateCaptor.getAllValues();
        assertThat(updatedMembers.get(0)).isNotNull();
        assertThat(updatedMembers.get(0).getSalesforceId()).isEqualTo("0011000001XYZ01");
        assertThat(updatedMembers.get(0).getClientName()).isEqualTo("Global Research University");
        assertThat(updatedMembers.get(0).isActive()).isTrue();
        assertThat(updatedMembers.get(0).getActivatedDate()).isNull();
        assertThat(updatedMembers.get(0).getDeactivatedDate()).isNull();
        assertThat(updatedMembers.get(1)).isNotNull();
        assertThat(updatedMembers.get(1).getSalesforceId()).isEqualTo("0011000002XYZ02");
        assertThat(updatedMembers.get(1).getClientName()).isEqualTo("Consortium Sub-Member A");
        assertThat(updatedMembers.get(1).isActive()).isTrue();
        assertThat(updatedMembers.get(1).getActivatedDate()).isNull(); // hasn't just been activated
        assertThat(updatedMembers.get(1).getLastModifiedDate()).isNotNull();
        assertThat(updatedMembers.get(1).getLastModifiedBy()).isEqualTo(SalesforceService.SALESFORCE_SYNC_USERNAME);
        assertThat(updatedMembers.get(1).getDeactivatedDate()).isNull();
        assertThat(updatedMembers.get(2)).isNotNull();
        assertThat(updatedMembers.get(2).getSalesforceId()).isEqualTo("0011000003XYZ03");
        assertThat(updatedMembers.get(2).getClientName()).isEqualTo("Consortium Sub-Member B");
        assertThat(updatedMembers.get(2).isActive()).isTrue();
        assertThat(updatedMembers.get(2).getActivatedDate()).isNull();
        assertThat(updatedMembers.get(2).getDeactivatedDate()).isNull();
        assertThat(updatedMembers.get(3)).isNotNull();
        assertThat(updatedMembers.get(3).getSalesforceId()).isEqualTo("0011000004XYZ04");
        assertThat(updatedMembers.get(3).getClientName()).isEqualTo("Legacy Research Lab");
        assertThat(updatedMembers.get(3).isActive()).isFalse();
        assertThat(updatedMembers.get(3).getDeactivatedDate()).isNull();
        assertThat(updatedMembers.get(3).getActivatedDate()).isNull();
        assertThat(updatedMembers.get(4)).isNotNull();
        assertThat(updatedMembers.get(4).getSalesforceId()).isEqualTo("0011000005XYZ05");
        assertThat(updatedMembers.get(4).getClientName()).isEqualTo("New Horizon Publisher");
        assertThat(updatedMembers.get(4).isActive()).isTrue();
        assertThat(updatedMembers.get(4).getActivatedDate()).isNull();
        assertThat(updatedMembers.get(4).getDeactivatedDate()).isNull();
    }

    @Test
    void testSyncMembers_createCL() throws IOException {
        Member nonCL = getMember("0011000001XYZ01", true);
        nonCL.setIsConsortiumLead(false);

        when(salesforceClient.getMembers()).thenReturn(getFileContent("src/test/resources/salesforce/members.json"));
        when(salesforceClient.getConsortium(eq("0011000001XYZ01"))).thenReturn(getFileContent("src/test/resources/salesforce/consortium.json"));
        when(memberService.getMember(eq("0011000001XYZ01"))).thenReturn(Optional.empty());
        when(salesforceClient.getMemberDetails(eq("0011000001XYZ01"))).thenReturn(getFileContent("src/test/resources/salesforce/consortiumLead.json"));

        when(memberService.createMember(any(), anyString())).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setId("id");
            return member;
        });

        salesforceService.syncMembers();

        verify(memberService, times(5)).createMember(salesforceUpdateCaptor.capture(), anyString());
        verify(memberService).addParent(eq("some-consortium-member-id"), eq("0011000001XYZ01"), anyString());

        List<Member> createdMembers = salesforceUpdateCaptor.getAllValues();
        Member createdCL = createdMembers.get(0);

        assertThat(createdCL).isNotNull();
        assertThat(createdCL.getSalesforceId()).isEqualTo("0011000001XYZ01");
        assertThat(createdCL.getIsConsortiumLead()).isTrue();
    }

    @Test
    void testSyncMembers_createWithMainContact() throws IOException {
        when(salesforceClient.getMembers()).thenReturn(getFileContent("src/test/resources/salesforce/members.json"));
        when(salesforceClient.getConsortium(anyString())).thenReturn("{}");
        doAnswer(invocation -> invocation.getArgument(0)).when(memberService).createMember(Mockito.any(), anyString());

        when(memberService.getMember(eq("0011000001XYZ01"))).thenReturn(Optional.of(getMember("0011000001XYZ01", false)));
        when(userService.getUsersByMemberId(anyString())).thenReturn(new ArrayList<>());
        when(salesforceClient.getMemberContacts(Mockito.eq("0011000001XYZ01"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactRoles.json"));
        when(salesforceClient.getMemberContactData(eq("1"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName1.json"));
        when(salesforceClient.getMemberContactData(eq("2"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName2.json"));
        when(salesforceClient.getMemberContactData(eq("3"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName3.json"));
        when(salesforceClient.getMemberContactData(eq("4"))).thenReturn(getFileContent("src/test/resources/salesforce/memberContactName4.json"));

        salesforceService.syncMembers();

        verify(userService).createMainContactUser(userCaptor.capture());

        User captured = userCaptor.getValue();
        assertThat(captured.getCreatedBy()).isEqualTo(SalesforceService.SALESFORCE_SYNC_USERNAME);
        assertThat(captured.getMainContact()).isTrue();
        assertThat(captured.getLastModifiedDate()).isNotNull();
        assertThat(captured.getLastModifiedBy()).isEqualTo(SalesforceService.SALESFORCE_SYNC_USERNAME);
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

    private MemberUpdateData getPublicMemberDetails() {
        MemberUpdateData memberUpdateData = new MemberUpdateData();
        memberUpdateData.setPublicName("test member details");
        memberUpdateData.setWebsite("https://website.com");
        memberUpdateData.setOrgName("orgName");
        memberUpdateData.setDescription("test");
        memberUpdateData.setEmail("email@orcid.org");
        memberUpdateData.setSalesforceId("salesforceId");
        return memberUpdateData;
    }

    private User getUser() {
        User user = new User();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        user.setMemberId("memberId");
        user.setMemberName("member");
        return user;
    }

    private Member getMember(String salesforceId, boolean active) {
        Member member = new Member();
        member.setSalesforceId(salesforceId);
        member.setActive(active);
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

    private ConsortiumLeadDetails getConsortiumLeadDetails() {
        ConsortiumLeadDetails consortiumLeadDetails = new ConsortiumLeadDetails();
        consortiumLeadDetails.setBillingCountry("Denmark");
        consortiumLeadDetails.setConsortiaLeadId(null);
        consortiumLeadDetails.setConsortiaMember(false);
        consortiumLeadDetails.setId("id");
        consortiumLeadDetails.setLogoUrl("some/url/for/a/logo");
        consortiumLeadDetails.setMemberType("Research Institute");
        consortiumLeadDetails.setName("test member details");
        consortiumLeadDetails.setPublicDisplayDescriptionHtml("<p>public display description</p>");
        consortiumLeadDetails.setPublicDisplayEmail("orcid@testmember.com");
        consortiumLeadDetails.setPublicDisplayName("public display name");
        consortiumLeadDetails.setMembershipStartDateString("2022-01-01");
        consortiumLeadDetails.setMembershipEndDateString("2027-01-01");
        consortiumLeadDetails.setWebsite("https://website.com");

        ConsortiumMember member1 = new ConsortiumMember();
        member1.setSalesforceId("member1");
        ConsortiumMember.Metadata metadata1 = member1.new Metadata();
        metadata1.setName("member 1");
        member1.setMetadata(metadata1);

        ConsortiumMember member2 = new ConsortiumMember();
        member2.setSalesforceId("member2");
        ConsortiumMember.Metadata metadata2 = member2.new Metadata();
        metadata2.setName("member 2");
        member2.setMetadata(metadata2);

        consortiumLeadDetails.setConsortiumMembers(Arrays.asList(member1, member2));
        return consortiumLeadDetails;
    }

    private MemberContacts getMemberContacts() {
        MemberContacts memberContacts = new MemberContacts();

        MemberContact contact1 = new MemberContact();
        contact1.setName("contact 1");
        contact1.setTitle("Dr");
        contact1.setPhone("123456789");
        contact1.setEmail("contact1@orcid.org");
        contact1.setRole("contact one role");
        contact1.setSalesforceId("salesforce-id");
        contact1.setVotingContact(false);

        MemberContact contact2 = new MemberContact();
        contact2.setName("contact 2");
        contact2.setEmail("contact2@orcid.org");
        contact2.setRole("contact two role");
        contact2.setSalesforceId("salesforce-id");
        contact2.setVotingContact(true);

        memberContacts.setTotalSize(2);
        memberContacts.setRecords(Arrays.asList(contact1, contact2));

        return memberContacts;
    }

    private User getCLUser() {
        User user = new User();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        user.setMemberId("parentMemberId");
        user.setMemberName("member");
        return user;
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

    private Member getConsortiumLeadMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setClientId("XXXX-XXXX-XXXX-XXXX");
        member.setClientName("clientname");
        member.setIsConsortiumLead(true);
        member.setSalesforceId("salesforceId");
        return member;
    }

    private Map<String, Object> getTestMetadata() throws IOException {
        String metadata = getFileContent("src/test/resources/salesforce/countryMetadata.json");
        Map<String, Object> metamap = new ObjectMapper().readValue(metadata, new TypeReference<Map<String, Object>>() {
        });
        return metamap;
    }

    private String getFileContent(String path) throws IOException {
        File consortiumLeadIds = new File(path);
        Path filePath = Path.of(consortiumLeadIds.getAbsolutePath());
        return Files.readString(filePath);
    }
}
