package org.orcid.mp.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.orcid.mp.member.client.SalesforceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.orcid.mp.member.salesforce.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesforceService {

    static final String MAIN_CONTACT_ROLE = "Main relationship contact (OFFICIAL)";

    static final String SALESFORCE_SYNC_USERNAME = "salesforce-sync";

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceService.class);

    private static final List<String> OPPORTUNITY_PUBLIC_STAGE_NAMES = List.of("Invoice Paid", "Agreement Signed", "Invoice Sent", "Partial Payment", "In Collections");

    private SalesforceClient salesforceClient;

    private ObjectMapper objectMapper;

    private List<Country> salesforceCountries;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Autowired
    private MemberService memberService;

    public SalesforceService(MemberService memberService, UserService userService, MailService mailService, SalesforceClient salesforceClient, ObjectMapper objectMapper) {
        this.salesforceClient = salesforceClient;
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.mailService = mailService;
        this.memberService = memberService;
    }

    public void processMemberContact(MemberContactUpdate memberContactUpdate, String salesforceId)  {
        User user = userService.getLoggedInUser();
        memberContactUpdate.setRequestedByEmail(user.getEmail());
        memberContactUpdate.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        memberContactUpdate.setRequestedByMember(user.getMemberName());

        if (memberContactUpdate.getContactEmail() == null) {
            mailService.sendAddContactEmail(memberContactUpdate);
        } else if (memberContactUpdate.getContactNewEmail() == null
                && memberContactUpdate.getContactNewName() == null
                && memberContactUpdate.getContactNewPhone() == null
                && memberContactUpdate.getContactNewRoles() == null
                && memberContactUpdate.getContactNewJobTitle() == null) {
            // no new data, must be remove operation
            mailService.sendRemoveContactEmail(memberContactUpdate);
        } else {
            mailService.sendUpdateContactEmail(memberContactUpdate);
        }
    }

    public void requestNewConsortiumMember(AddConsortiumMember addConsortiumMember) {
        User user = userService.getLoggedInUser();

        Optional<Member> optionalMember = memberService.getMember(user.getMemberId());
        Member member = optionalMember.get();
        if (!member.getIsConsortiumLead()) {
            throw new RuntimeException("Requesting member is not a consortium lead");
        }

        addConsortiumMember.setRequestedByEmail(user.getEmail());
        addConsortiumMember.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        addConsortiumMember.setConsortium(user.getMemberName());

        mailService.sendAddConsortiumMemberEmail(addConsortiumMember);
    }

    public void requestRemoveConsortiumMember(RemoveConsortiumMember removeConsortiumMember) {
        User user = userService.getLoggedInUser();
        LOG.info("Requesting remove consortium member {} by user {}", removeConsortiumMember.getOrgName(), user.getEmail());

        Optional<Member> optionalMember = memberService.getMember(user.getMemberId());
        Member member = optionalMember.get();
        if (!member.getIsConsortiumLead()) {
            throw new RuntimeException("Requesting member is not a consortium lead");
        }

        removeConsortiumMember.setRequestedByEmail(user.getEmail());
        removeConsortiumMember.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        removeConsortiumMember.setConsortium(user.getMemberName());
        mailService.sendRemoveConsortiumMemberEmail(removeConsortiumMember);
    }

    public MemberDetails getMemberDetails(String salesforceId) {
        JsonNode memberElement = getMemberJson(salesforceId);
        if (memberElement != null) {
            try {
                return objectMapper.treeToValue(memberElement, MemberDetails.class);
            } catch (JsonProcessingException e) {
                LOG.warn("Error writing member JSON to MemberDetails for salesforce id {}", salesforceId, e);
                return null;
            }
        }
        return null;
    }

    public ConsortiumLeadDetails getConsortiumLeadDetails(String salesforceId) {
        JsonNode memberElement = getMemberJson(salesforceId);
        if (memberElement != null) {
            ArrayNode consortiumMembers = getConsortiumMembersJson(salesforceId);
            if (consortiumMembers == null) {
                LOG.debug("No consortium members found for salesforce id {}", salesforceId);
                return null;
            }
            try {
                ConsortiumLeadDetails consortiumLeadDetails = objectMapper.treeToValue(memberElement, ConsortiumLeadDetails.class);
                List<ConsortiumMember> members = objectMapper.treeToValue(consortiumMembers, new TypeReference<List<ConsortiumMember>>() {});
                consortiumLeadDetails.setConsortiumMembers(members);
                return consortiumLeadDetails;
            } catch (JsonProcessingException e) {
                LOG.warn("Error writing member JSON to ConsortiumLeadDetails for salesforce id {}", salesforceId, e);
                return null;
            }
        }
        return null;
    }

    public List<Country> getSalesforceCountries() {
        if (salesforceCountries == null) {
            salesforceCountries = generateSalesforceCountries();
        }
        return salesforceCountries;
    }

    public MemberContacts getMemberContacts(String salesforceId) {
        ObjectNode contactsJson = getMemberContactsJson(salesforceId);
        if (contactsJson != null) {
            try {
                return objectMapper.treeToValue(contactsJson, MemberContacts.class);
            } catch (JsonProcessingException e) {
                LOG.warn("Error writing member JSON to MemberContacts for salesforce id {}", salesforceId, e);
            }
        }
        return null;
    }

    public MemberOrgIds getMemberOrgIds(String salesforceId) {
        String memberOrgIdsJson = salesforceClient.getMemberOrgIds(salesforceId);
        if (memberOrgIdsJson != null) {
            try {
                return objectMapper.readValue(memberOrgIdsJson, MemberOrgIds.class);
            } catch (JsonProcessingException e) {
                LOG.warn("Error writing member JSON to MemberOrgIds for salesforce id {}", salesforceId, e);
            }
        }
        return null;
    }

    public void updatePublicMemberDetails(MemberUpdateData memberUpdateData) {
        salesforceClient.updatePublicMemberDetails(memberUpdateData);
    }

    public void syncMembers() {
        try {
            List<MemberDetails> members = getAllMembers();
            members.forEach(this::syncMember);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to get active members from Salesforce", e);
            throw new RuntimeException(e);
        }
    }

    private void syncMember(MemberDetails salesforceMemberData) {
        LOG.info("Syncing member with SF ID {}", salesforceMemberData.getId());
        try {
            processSalesforceMemberData(salesforceMemberData);
            processSalesforceContactData(salesforceMemberData);
        } catch (Exception e) {
            LOG.error("Failed to sync member {}", salesforceMemberData.getId(), e);
        }
    }

    private void processSalesforceContactData(MemberDetails salesforceMemberData) {
        Optional<Member> member = memberService.getMember(salesforceMemberData.getId());
        if (member.isPresent()) {
            List<User> users = userService.getUsersByMemberId(member.get().getId());
            if (users == null || users.isEmpty()) {
                MemberContact mainContact = getMainContact(salesforceMemberData.getId());
                User user = getUserForMainContact(mainContact, member.get().getId());
                userService.createMainContactUser(user);
            }
        } else {
            LOG.warn("Cannot process member contacts for {} because member record has not been created", salesforceMemberData.getId());
        }
    }

    private User getUserForMainContact(MemberContact mainContact, String memberId) {
        User user = new User();
        user.setMemberId(memberId);
        user.setFirstName(mainContact.getName().split(" ")[0]);
        user.setLastName(mainContact.getName().split(" ")[1]);
        user.setEmail(mainContact.getEmail());
        user.setMainContact(true);
        user.setCreatedBy(SALESFORCE_SYNC_USERNAME);
        user.setCreatedDate(Instant.now());
        user.setLastModifiedBy(SALESFORCE_SYNC_USERNAME);
        user.setLastModifiedDate(Instant.now());
        return user;
    }

    private MemberContact getMainContact(String id) {
        MemberContacts memberContacts = getMemberContacts(id);
        if (memberContacts != null && memberContacts.getRecords() != null) {
            return memberContacts.getRecords().stream()
                    .filter(contact -> MAIN_CONTACT_ROLE.equals(contact.getRole()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void processSalesforceMemberData(MemberDetails salesforceMemberData) {
        ConsortiumLeadDetails consortiumData = getConsortiumLeadDetails(salesforceMemberData.getId());
        Optional<Member> existingMemberRecord = memberService.getMember(salesforceMemberData.getId());
        if (existingMemberRecord.isPresent()) {
            LOG.debug("Found existing member {}", salesforceMemberData.getId());
            updateExistingMemberWithSalesforceData(existingMemberRecord.get(), salesforceMemberData, consortiumData != null);
            if (consortiumData != null) {
                updateParentForConsortiumMembers(consortiumData);
            } else if (consortiumData == null && existingMemberRecord.get().getIsConsortiumLead()) {
                // member no longer consortium lead
                removeParentFromConsortiumMembers(consortiumData);
            }
        } else {
            LOG.debug("Member {} not found", salesforceMemberData.getId());
            createNewMemberWithSalesforceData(salesforceMemberData, consortiumData != null);
            if (consortiumData != null) {
                updateParentForConsortiumMembers(consortiumData);
            }
        }
    }

    private void updateParentForConsortiumMembers(ConsortiumLeadDetails consortiumData) {
        consortiumData.getConsortiumMembers().forEach(consortiumMember -> {
            memberService.addParent(consortiumMember.getSalesforceId(), consortiumData.getId());
        });

        Set<String> activeConsortiumIds = consortiumData.getConsortiumMembers().stream()
                .map(ConsortiumMember::getSalesforceId)
                .collect(Collectors.toSet());

        memberService.removeParentFromMembersNoLongerPartOfConsortium(consortiumData.getMemberId(), activeConsortiumIds);
    }

    private void removeParentFromConsortiumMembers(ConsortiumLeadDetails consortiumData) {
        consortiumData.getConsortiumMembers().forEach(consortiumMember -> {
            memberService.removeParent(consortiumMember.getMemberId());
        });
    }

    private void createNewMemberWithSalesforceData(MemberDetails salesforceMemberData, boolean consortiumLead) {
        Member member = new Member();
        member.setSalesforceId(salesforceMemberData.getId());
        member.setActive(salesforceMemberData.isActiveMember());
        member.setClientName(salesforceMemberData.getName());
        member.setAssertionServiceEnabled(false);
        member.setIsConsortiumLead(consortiumLead);
        member = memberService.createMember(member, SALESFORCE_SYNC_USERNAME);
        LOG.info("Created new member {}", member.getId());
    }

    private void updateExistingMemberWithSalesforceData(Member member, MemberDetails salesforceMemberData, boolean consortiumLead) {
        member = updateMemberMetadata(member, salesforceMemberData, consortiumLead);
        member = updateMemberStatus(member, salesforceMemberData);
        member.setLastUpdatedWithSalesforceData(Instant.now());
        memberService.updateMember(member, SALESFORCE_SYNC_USERNAME);
    }

    private Member updateMemberStatus(Member member, MemberDetails salesforceMemberData) {
        if (salesforceMemberData.isActiveMember() && !member.isActive()) {
            LOG.info("Activating member {}", member.getId());
            member.setActive(true);
            member.setActivatedDate(Instant.now());
        } else if (!salesforceMemberData.isActiveMember() && member.isActive()) {
            LOG.info("Deactivating member {}", member.getId());
            member.setActive(false);
            member.setDeactivatedDate(Instant.now());
        }
        return member;
    }

    private Member updateMemberMetadata(Member member, MemberDetails salesforceMemberData, boolean consortiumLead) {
        LOG.debug("SF sync setting member {} name to {}", member.getId(), salesforceMemberData.getName());
        member.setClientName(salesforceMemberData.getName());
        member.setIsConsortiumLead(consortiumLead);
        return member;
    }

    private List<MemberDetails> getAllMembers() throws JsonProcessingException {
        List<MemberDetails> activeMembers = new ArrayList<>();
        String activeMembersJson = salesforceClient.getMembers();

        MembersPage membersPage = objectMapper.readValue(activeMembersJson, MembersPage.class);
        activeMembers.addAll(membersPage.getRecords());

        while (!membersPage.isDone()) {
            activeMembersJson = salesforceClient.fetchDataFromUrl(membersPage.getNextRecordsUrl());
            membersPage = objectMapper.readValue(activeMembersJson, MembersPage.class);
            activeMembers.addAll(membersPage.getRecords());
        }
        return activeMembers;
    }

    private Map<String, Object> getDataMapForUpdate(MemberUpdateData memberData) {
        Map<String, Object> data = new HashMap<>();
        data.put("Name", memberData.getOrgName());
        data.put("Public_Display_Name__c", memberData.getPublicName());
        data.put("Public_Display_Description__c", memberData.getDescription());
        data.put("Public_Display_Email__c", memberData.getEmail());
        data.put("Website", memberData.getWebsite());

        if (memberData.getTrademarkLicense() != null) {
            data.put("Trademark_License__c", memberData.getTrademarkLicense());
        }

        if (memberData.getBillingAddress() != null) {
            data.put("BillingCity", memberData.getBillingAddress().getCity());
            data.put("BillingCountry", memberData.getBillingAddress().getCountry());
            data.put("BillingCountryCode", memberData.getBillingAddress().getCountryCode());
            data.put("BillingPostalCode", memberData.getBillingAddress().getPostalCode());
            data.put("BillingState", memberData.getBillingAddress().getState());
            data.put("BillingStateCode", memberData.getBillingAddress().getStateCode());
            data.put("BillingStreet", memberData.getBillingAddress().getStreet());
        }
        return data;
    }

    private ObjectNode getMemberContactsJson(String salesforceId) {
        try {
            String contactsResponseString = salesforceClient.getMemberContacts(salesforceId);

            // parse and cast to ObjectNode so we can modify it later
            ObjectNode contactsObject = (ObjectNode) objectMapper.readTree(contactsResponseString);
            JsonNode contactsArray = contactsObject.path("records");

            for (JsonNode c : contactsArray) {
                // cast to ObjectNode so we can add properties to it
                ObjectNode contactObject = (ObjectNode) c;
                String contactId = contactObject.path("Contact__c").asText();
                String contactNameResponseString = salesforceClient.getMemberContactData(contactId);
                JsonNode contactNamesObject = objectMapper.readTree(contactNameResponseString);
                JsonNode contactNamesArray = contactNamesObject.path("records");

                if (!contactNamesArray.isEmpty()) {
                    JsonNode firstContactNameObject = contactNamesArray.get(0);

                    String name = firstContactNameObject.path("Name").asText();
                    contactObject.put("Name", name);

                    if (firstContactNameObject.hasNonNull("Title")) {
                        contactObject.put("Title", firstContactNameObject.path("Title").asText());
                    }

                    if (firstContactNameObject.hasNonNull("Phone")) {
                        contactObject.put("Phone", firstContactNameObject.path("Phone").asText());
                    }
                }
            }
            return contactsObject;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse contacts JSON for memberId: " + salesforceId, e);
        }
    }

    private JsonNode getMemberJson(String salesforceId) {
        String memberJson = salesforceClient.getMemberDetails(salesforceId);
        if (memberJson == null || memberJson.isBlank()) {
            LOG.warn("Empty response from salesforce for salesforceId {}", salesforceId);
            return null;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(memberJson);
            JsonNode records = rootNode.path("records");
            if (records.isMissingNode() || records.isEmpty()) {
                LOG.warn("No records found for salesforce id {}", salesforceId);
                return null;
            }
            return records.get(0);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse Salesforce JSON response for ID: {}", salesforceId, e);
            return null;
        }
    }

    private ArrayNode getConsortiumMembersJson(String salesforceId) {
        String consortiumJson = salesforceClient.getConsortium(salesforceId);
        try {
            JsonNode consortiumNode = objectMapper.readTree(consortiumJson);
            JsonNode consortiumRecords = (consortiumJson != null) ? consortiumNode.path("records") : null;
            JsonNode firstElement = (consortiumRecords != null && !consortiumRecords.isEmpty()) ? consortiumRecords.get(0) : null;
            JsonNode consortiumOpportunitiesJson = (firstElement != null) ? firstElement.path("ConsortiaOpportunities__r") : null;
            if (consortiumOpportunitiesJson != null && consortiumOpportunitiesJson.hasNonNull("records")) {
                JsonNode opportunitiesArray = consortiumOpportunitiesJson.get("records");
                if (opportunitiesArray.isArray()) {
                    ArrayNode publicOpportunitiesArray = objectMapper.createArrayNode();
                    for (JsonNode opportunity : opportunitiesArray) {
                        if (opportunity.hasNonNull("StageName")) {
                            String stageName = opportunity.get("StageName").asText();
                            if (OPPORTUNITY_PUBLIC_STAGE_NAMES.contains(stageName)) {
                                publicOpportunitiesArray.add(opportunity);
                            }
                        }
                    }
                    return publicOpportunitiesArray;
                } else {
                    LOG.warn("Consortium opportunities JSON is not an array for salesforce id {}", salesforceId);
                }
            } else {
                LOG.debug("No consortium opportunities found for salesforce id {}", salesforceId);
            }
        } catch (JsonProcessingException e) {
            LOG.warn("Error processing consortium JSON from Salesforce for salesforce id {}", salesforceId, e);
        }
        return null;
    }

    private List<Country> generateSalesforceCountries() {
        List<Map<String, Object>> metadata = getRelevantMetadata();
        List<Country> salesforceCountries = getSalesforceCountries(metadata);
        populateSalesforceCountryStates(salesforceCountries, metadata);
        return salesforceCountries;
    }

    private void populateSalesforceCountryStates(List<Country> salesforceCountries, List<Map<String, Object>> fields) {
        fields.forEach(f -> {
            if (f.get("name").equals("BillingStateCode")) {
                List<Map<String, Object>> states = (List<Map<String, Object>>) f.get("picklistValues");
                states.forEach(s -> {
                    State state = new State();
                    state.setCode((String) s.get("value"));
                    state.setName((String) s.get("label"));

                    int countryIndex = getIndexOfCountry((String) s.get("validFor"));
                    Country country = salesforceCountries.get(countryIndex);
                    if (country.getStates() == null) {
                        country.setStates(new ArrayList<>());
                    }
                    country.getStates().add(state);
                });
            }
        });
    }

    private int getIndexOfCountry(String validFor) {
        byte[] bytes = Base64.getDecoder().decode(validFor);
        for (int index = 0; index < bytes.length; index++) {
            byte b = bytes[index];
            for (int shift = 7; shift >= 0; shift--) {
                if ((b & 1 << shift) > 0) {
                    // bit is set
                    return index * 8 + (7 - shift);
                }
            }
        }
        LOG.warn("Can't find country index from validFrom variable");
        return -1;
    }

    private List<Country> getSalesforceCountries(List<Map<String, Object>> fields) {
        List<Country> salesforceCountries = new ArrayList<>();
        fields.forEach(f -> {
            if (f.get("name").equals("BillingCountryCode")) {
                List<Map<String, Object>> countries = (List<Map<String, Object>>) f.get("picklistValues");
                countries.forEach(c -> {
                    Country country = new Country();
                    country.setCode((String) c.get("value"));
                    country.setName((String) c.get("label"));
                    salesforceCountries.add(country);
                });
            }
        });
        return salesforceCountries;
    }

    private List<Map<String, Object>> getRelevantMetadata() {
        Map<String, Object> salesforceMetadata = salesforceClient.getMetadata();
        List<Map<String, Object>> fields = (List<Map<String, Object>>) salesforceMetadata.get("fields");
        return fields;
    }

}
