import { Moment } from 'moment';

export interface IMSMember {
  id?: string;
  clientId?: string;
  clientName?: string;
  clientSecret?: string;
  salesforceId?: string;
  parentSalesforceId?: string;
  isConsortiumLead?: boolean;
  superadminEnabled?: boolean;
  assertionServiceEnabled?: boolean;
  createdBy?: string;
  createdDate?: Moment;
  lastModifiedBy?: string;
  lastModifiedDate?: Moment;
}

export class MSMember implements IMSMember {
  constructor(
    public id?: string,
    public clientId?: string,
    public clientName?: string,
    public clientSecret?: string,
    public salesforceId?: string,
    public parentSalesforceId?: string,
    public isConsortiumLead?: boolean,
    public superadminEnabled?: boolean,
    public assertionServiceEnabled?: boolean,
    public createdBy?: string,
    public createdDate?: Moment,
    public lastModifiedBy?: string,
    public lastModifiedDate?: Moment
  ) {
    this.isConsortiumLead = this.isConsortiumLead || false;
    this.superadminEnabled = this.superadminEnabled || false;
    this.assertionServiceEnabled = this.assertionServiceEnabled || false;

  }
}
