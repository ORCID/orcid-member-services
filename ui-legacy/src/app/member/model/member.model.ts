import { Moment } from 'moment'

export interface IMember {
  id?: string
  clientId?: string
  clientName?: string
  clientSecret?: string
  salesforceId?: string
  parentSalesforceId?: string
  isConsortiumLead?: boolean
  superadminEnabled?: boolean
  assertionServiceEnabled?: boolean
  createdBy?: string
  createdDate?: Moment | undefined | null
  lastModifiedBy?: string
  lastModifiedDate?: Moment | undefined | null
  type?: string
  status?: string
  defaultLanguage?: string
}

export class Member implements IMember {
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
    public createdDate?: Moment | undefined | null,
    public lastModifiedBy?: string,
    public lastModifiedDate?: Moment | undefined | null,
    public type?: string,
    public status?: string,
    public defaultLaungage?: string
  ) {
    this.isConsortiumLead = this.isConsortiumLead || false
    this.superadminEnabled = this.superadminEnabled || false
    this.assertionServiceEnabled = this.assertionServiceEnabled || false
  }
}
