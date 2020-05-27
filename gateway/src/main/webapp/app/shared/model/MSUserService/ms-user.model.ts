import { Moment } from 'moment';

export const enum UserAuthorities {
  ROLE_USER = 'ROLE_USER',
  CONSORTIUM_LEAD = 'CONSORTIUM_LEAD',
  ASSERTION_SERVICE_ENABLED = 'ASSERTION_SERVICE_ENABLED'
}

export interface IMSUser {
  id?: string;
  login?: string;
  firstName?: string;
  lastName?: string;
  mainContact?: boolean;
  salesforceId?: string;
  parentSalesforceId?: string;
  createdBy?: string;
  createdDate?: Moment;
  lastModifiedBy?: string;
  lastModifiedDate?: Moment;
}

export class MSUser implements IMSUser {
  constructor(
    public id?: string,
    public login?: string,
    public firstName?: string,
    public lastName?: string,
    public mainContact?: boolean,
    public salesforceId?: string,
    public parentSalesforceId?: string,
    public createdBy?: string,
    public createdDate?: Moment,
    public lastModifiedBy?: string,
    public lastModifiedDate?: Moment
  ) {
    this.mainContact = this.mainContact || false;
  }
}
