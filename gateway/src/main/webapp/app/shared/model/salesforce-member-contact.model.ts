export interface ISFMemberContact {
  memberId?: string;
  votingContant?: boolean;
  memberOrgRole?: string[];
  name?: string;
  lastName?: string;
  contactEmail?: string;
  title?: string;
  phone?: string;
}

export interface ISFRawMemberContact {
  Organization__c?: string;
  Voting_Contact__c?: boolean;
  Contact_Curr_Email__c?: string;
  Member_Org_Role__c?: string;
  Name?: string;
  Title?: string;
  Phone?: string;
}

export interface ISFRawMemberContacts {
  size?: number;
  records?: ISFRawMemberContact[];
}

export class SFMemberContact implements ISFMemberContact {
  constructor(
    public memberId?: string,
    public votingContact?: boolean,
    public memberOrgRole?: string[],
    public name?: string,
    public lastName?: string,
    public contactEmail?: string,
    public title?: string,
    public phone?: string
  ) {
    this.memberOrgRole = [];
  }
}
