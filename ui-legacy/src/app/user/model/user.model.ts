import { Moment } from 'moment'

export const enum UserAuthorities {
  ROLE_USER = 'ROLE_USER',
  ROLE_ADMIN = 'ROLE_ADMIN',
  CONSORTIUM_LEAD = 'CONSORTIUM_LEAD',
  ASSERTION_SERVICE_ENABLED = 'ASSERTION_SERVICE_ENABLED',
}

export interface IUser {
  id?: string
  email?: string
  firstName?: string
  lastName?: string
  mainContact?: boolean
  salesforceId?: string
  memberName?: string
  parentSalesforceId?: string
  activated?: boolean
  isAdmin?: boolean
  authorities?: [string]
  createdBy?: string
  createdDate?: Moment | undefined | null
  lastModifiedBy?: string
  lastModifiedDate?: Moment | undefined | null
  mfaEnabled?: boolean
}

export class User implements IUser {
  constructor(
    public id?: string,
    public email?: string,
    public firstName?: string,
    public lastName?: string,
    public mainContact?: boolean,
    public salesforceId?: string,
    public memberName?: string,
    public parentSalesforceId?: string,
    public createdBy?: string,
    public createdDate?: Moment | undefined | null,
    public lastModifiedBy?: string,
    public activated?: boolean,
    public isAdmin?: boolean,
    public lastModifiedDate?: Moment | undefined | null,
    public mfaEnabled?: boolean,
    public authorities?: [string]
  ) {
    this.isAdmin = this.isAdmin || false
    this.mainContact = this.mainContact || false
    this.activated = this.activated || false
    this.mfaEnabled = this.mfaEnabled || false
  }
}
