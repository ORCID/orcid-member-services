import { Moment } from 'moment';

export interface IUserSettings {
  id?: string;
  login?: string;
  email?: string;
  password?: string;
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

export class UserSettings implements IUserSettings {
  constructor(
    public id?: string,
    public login?: string,
    public email?: string,
    public password?: string,
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
