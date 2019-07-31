export interface IMemberServicesUser {
  id?: string;
  user_id?: string;
  salesforceId?: string;
  parentSalesforceId?: string;
  disabled?: boolean;
  mainContact?: boolean;
  assertionServiceEnabled?: boolean;
  oboClientId?: string;
}

export class MemberServicesUser implements IMemberServicesUser {
  constructor(
    public id?: string,
    public user_id?: string,
    public salesforceId?: string,
    public parentSalesforceId?: string,
    public disabled?: boolean,
    public mainContact?: boolean,
    public assertionServiceEnabled?: boolean,
    public oboClientId?: string
  ) {
    this.disabled = this.disabled || false;
    this.mainContact = this.mainContact || false;
    this.assertionServiceEnabled = this.assertionServiceEnabled || false;
  }
}
