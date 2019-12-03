import { Moment } from 'moment';

export interface IUserSettings {
  id?: string;
  login?: string;
  salesforceId?: string;
  disabled?: boolean;
  assertionsServiceDisabled?: boolean;
  mainContact?: boolean;
  createdBy?: string;
  createdDate?: Moment;
  lastModifiedBy?: string;
  lastModifiedDate?: Moment;
}

export class UserSettings implements IUserSettings {
  constructor(
    public id?: string,
    public login?: string,
    public salesforceId?: string,
    public disabled?: boolean,
    public assertionsServiceDisabled?: boolean,
    public mainContact?: boolean,
    public createdBy?: string,
    public createdDate?: Moment,
    public lastModifiedBy?: string,
    public lastModifiedDate?: Moment
  ) {
    this.disabled = this.disabled || false;
    this.mainContact = this.mainContact || false;
  }
}
