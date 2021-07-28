import { Moment } from 'moment';

export const enum UserAuthorities {
  ROLE_USER = 'ROLE_USER',
  ROLE_ADMIN = 'ROLE_ADMIN',
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
  memberName?: string;
  parentSalesforceId?: string;
  activated?: boolean;
  isAdmin?: boolean;
  authorities?: [string];
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
    public memberName?: string,
    public parentSalesforceId?: string,
    public createdBy?: string,
    public createdDate?: Moment,
    public lastModifiedBy?: string,
    public activated?: boolean,
    public isAdmin?: boolean,
    public lastModifiedDate?: Moment
  ) {
    this.isAdmin = this.isAdmin || false;
    this.mainContact = this.mainContact || false;
    this.activated = this.activated || false;
  }
}
