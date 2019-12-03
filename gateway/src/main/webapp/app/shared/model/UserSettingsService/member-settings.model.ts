import { Moment } from 'moment';

export interface IMemberSettings {
  id?: string;
  clientId?: string;
  clientSecret?: string;
  salesforceId?: string;
  parentSalesforceId?: string;
  isConsortiumLead?: boolean;
  assertionServiceEnabled?: boolean;
  createdBy?: string;
  createdDate?: Moment;
  lastModifiedBy?: string;
  lastModifiedDate?: Moment;
}

export class MemberSettings implements IMemberSettings {
  constructor(
    public id?: string,
    public clientId?: string,
    public clientSecret?: string,
    public salesforceId?: string,
    public parentSalesforceId?: string,
    public isConsortiumLead?: boolean,
    public assertionServiceEnabled?: boolean,
    public createdBy?: string,
    public createdDate?: Moment,
    public lastModifiedBy?: string,
    public lastModifiedDate?: Moment
  ) {
    this.isConsortiumLead = this.isConsortiumLead || false;
    this.assertionServiceEnabled = this.assertionServiceEnabled || false;
  }
}
