export interface ILoginResult {
  mfaRequired: boolean
  oauth2AccessToken?: any
}

export interface ILoginCredentials {
  username?: string
  password?: string
  mfaCode?: string | null
}
