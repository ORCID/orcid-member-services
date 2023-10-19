export interface ISFMemberOrgIds {
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

export class SFMemberOrgIds implements ISFMemberOrgIds {
  constructor(public ROR?: string[], public GRID?: string[], public Ringgold?: string[], public Fundref?: string[]) {}
}
