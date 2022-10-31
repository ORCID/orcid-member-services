import { SFMemberContact } from './salesforce-member-contact.model copy';
import { SFMemberOrgId } from './salesforce-member-org-id.model copy';

export interface ISFMemberData {
  id?: string;
  consortiaMember?: boolean;
  consortiaLeadId?: string;
  isConsortiumLead?: boolean;
  name?: string;
  publicDisplayName?: string;
  website?: string;
  billingCountry?: string;
  memberType?: string;
  publicDisplayDescriptionHtml?: string;
  logoUrl?: string;
  publicDisplayEmail?: string;
  membershipStartDateString?: string;
  membershipEndDateString?: string;
  consortiumLeadName?: string;
  consortiumMembers?: ISFConsortiumMemberData[];
  contacts?: SFMemberContact[];
  orgIds?: SFMemberOrgId;
}

export interface ISFConsortiumMemberData {
  name?: string;
  salesforceId?: string;
  metadata?: ISFConsortiumMemberMetadata;
}

export interface ISFConsortiumMemberMetadata {
  name?: string;
}

export interface ISFRawMemberData {
  Id?: string;
  Consortia_Member__c?: boolean;
  Consortium_Lead__c?: string;
  Name?: string;
  Public_Display_Name__c?: string;
  Website?: string;
  BillingCountry?: string;
  Research_Community__c?: string;
  Public_Display_Description__c?: string;
  Logo_Description__c?: string;
  Public_Display_Email__c?: string;
  Last_membership_start_date__c?: string;
  Last_membership_end_date__c?: string;
  consortiumOpportunities: [ISFRawConsortiumMemberData];
}

export interface ISFRawConsortiumMemberData {
  AccountId?: string;
  Account?: ISFRawConsortiumMemberMetadata;
}

export interface ISFRawConsortiumMemberMetadata {
  Public_Display_Name__c?: string;
}

export class SFMemberData implements ISFMemberData {
  constructor(
    public id?: string,
    public consortiaMember?: boolean,
    public consortiaLeadId?: string,
    public isConsortiumLead?: boolean,
    public name?: string,
    public publicDisplayName?: string,
    public website?: string,
    public billingCountry?: string,
    public memberType?: string,
    public publicDisplayDescriptionHtml?: string,
    public logoUrl?: string,
    public publicDisplayEmail?: string,
    public membershipStartDateString?: string,
    public membershipEndDateString?: string,
    public consortiumLeadName?: string,
    public consortiumMembers?: SFConsortiumMemberData[],
    public contacts?: SFMemberContact[],
    public orgIds?: SFMemberOrgId
  ) {}
}

export class SFConsortiumMemberData implements ISFConsortiumMemberData {
  constructor(public salesforceId?: string, public name?: string) {}
}
