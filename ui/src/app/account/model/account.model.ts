export interface IAccount {
  activated: boolean
  authorities: string[]
  email: string
  firstName: string
  langKey: string
  lastName: string
  imageUrl: string
  salesforceId: string
  loggedAs: boolean
  loginAs: string
  mainContact: boolean
  mfaEnabled: boolean
}
