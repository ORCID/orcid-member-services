import { Moment } from 'moment';

export interface IMSMember {
  id?: string | null;
  clientId?: string | null;
  clientName?: string | null;
  clientSecret?: string | null;
  salesforceId?: string | null;
  parentSalesforceId?: string | null;
  isConsortiumLead?: boolean;
  superadminEnabled?: boolean;
  assertionServiceEnabled?: boolean;
  createdBy?: string | null;
  createdDate?: Moment | null;
  lastModifiedBy?: string | null;
  lastModifiedDate?: Moment | null;
  type?: string | null;
  status?: string | null;
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
    public lastModifiedDate?: Moment,
    public type?: string,
    public status?: string
  ) {
    this.isConsortiumLead = this.isConsortiumLead || false;
    this.superadminEnabled = this.superadminEnabled || false;
    this.assertionServiceEnabled = this.assertionServiceEnabled || false;
  }
}
