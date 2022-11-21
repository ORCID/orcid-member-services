export interface IUser {
  id?: any;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  activated?: boolean;
  langKey?: string | null;
  authorities?: any[] | null;
  createdBy?: string | null;
  createdDate?: Date | null;
  lastModifiedBy?: string | null;
  lastModifiedDate?: Date | null;
  password?: string | null;
}

export class User implements IUser {
  constructor(
    public id?: any,
    public firstName?: string | null,
    public lastName?: string | null,
    public email?: string | null,
    public activated?: boolean,
    public langKey?: string | null,
    public authorities?: any[] | null,
    public createdBy?: string | null,
    public createdDate?: Date | null,
    public lastModifiedBy?: string | null,
    public lastModifiedDate?: Date | null,
    public password?: string | null
  ) {
    this.id = id ? id : null;
    this.firstName = firstName ? firstName : null;
    this.lastName = lastName ? lastName : null;
    this.email = email ? email : null;
    this.activated = activated ? activated : false;
    this.langKey = langKey ? langKey : null;
    this.authorities = authorities ? authorities : null;
    this.createdBy = createdBy ? createdBy : null;
    this.createdDate = createdDate ? createdDate : null;
    this.lastModifiedBy = lastModifiedBy ? lastModifiedBy : null;
    this.lastModifiedDate = lastModifiedDate ? lastModifiedDate : null;
    this.password = password ? password : null;
  }
}
