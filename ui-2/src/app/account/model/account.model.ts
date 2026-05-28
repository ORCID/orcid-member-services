export interface IAccount {
  id: string
  activated: boolean
  authorities: string[]
  email: string
  firstName: string
  langKey: string
  lastName: string
  imageUrl: string
  memberId: string
  loggedAs: boolean
  loginAs: string
  mainContact: boolean
  mfaEnabled: boolean
}
