export interface ISFMemberOrgId {
  ROR?: string[];
  GRID?: string[];
  Ringgold?: string[];
  Fundref?: string[];
}

export interface ISFRawMemberOrgId {
  Identifier_Type__c: string;
  Name: string;
}

export interface ISFRawMemberOrgIds {
  records?: ISFRawMemberOrgId[];
  size?: number;
  done?: boolean;
}

export class SFMemberOrgId implements ISFMemberOrgId {
  constructor(public ROR?: string[], public GRID?: string[], public Ringgold?: string[], public Fundref?: string[]) {}
}
