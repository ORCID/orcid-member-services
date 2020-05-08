import { Moment } from 'moment';

export const enum UserAuthorities {
  ROLE_USER = 'ROLE_USER',
  CONSORTIUM_LEAD = 'CONSORTIUM_LEAD',
  ASSERTION_SERVICE_ENABLED = 'ASSERTION_SERVICE_ENABLED'
}

export interface IUserSettings {
  id?: string;
  login?: string;
  firstName?: string;
  lastName?: string;
  mainContact?: boolean;
  assertionServicesEnabled?: boolean;
  salesforceId?: string;
  parentSalesforceId?: string;
  createdBy?: string;
  createdDate?: Moment;
  lastModifiedBy?: string;
  lastModifiedDate?: Moment;
}

export class UserSettings implements IUserSettings {
  constructor(
    public id?: string,
    public login?: string,
    public firstName?: string,
    public lastName?: string,
    public mainContact?: boolean,
    public assertionServicesEnabled?: boolean,
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
