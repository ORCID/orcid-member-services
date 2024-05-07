export interface ISFMemberContact {
  memberId?: string
  votingContant?: boolean
  memberOrgRole?: string[]
  name?: string
  contactEmail?: string
  title?: string
  phone?: string
}

export interface ISFRawMemberContact {
  Organization__c?: string
  Voting_Contact__c?: boolean
  Contact_Curr_Email__c?: string
  Member_Org_Role__c?: string
  Name?: string
  Title?: string
  Phone?: string
}

export interface ISFRawMemberContacts {
  totalSize?: number
  records?: ISFRawMemberContact[]
}

export class SFMemberContact implements ISFMemberContact {
  constructor(
    public memberId?: string,
    public votingContact?: boolean,
    public memberOrgRole?: string[],
    public name?: string,
    public contactEmail?: string,
    public title?: string,
    public phone?: string
  ) {}
}

export class ISFMemberContactUpdate {
  contactName?: string
  contactEmail?: string
  contactMember?: string
  contactNewName?: string
  contactNewEmail?: string
  contactNewJobTitle?: string
  contactNewPhone?: string
  contactNewRoles?: string[]
}

export class SFMemberContactUpdate implements ISFMemberContactUpdate {
  constructor(
    public contactName?: string,
    public contactEmail?: string,
    public contactMember?: string,
    public contactNewName?: string,
    public contactNewEmail?: string,
    public contactNewJobTitle?: string,
    public contactNewPhone?: string,
    public contactNewRoles?: string[]
  ) {}
}

export class ISFMemberContactRole {
  id?: number
  selected?: boolean
  name?: string
}

export class SFMemberContactRole implements ISFMemberContactRole {
  constructor(
    public id: number,
    public selected: boolean,
    public name: string
  ) {}
}
