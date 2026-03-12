export interface IPasswordResetInitResult {
  success: boolean
  emailNotFound: boolean
  generalError: boolean
}

export class PasswordResetInitResult implements IPasswordResetInitResult {
  success: boolean
  emailNotFound: boolean
  generalError: boolean

  constructor(success: boolean, emailNotFound: boolean, generalError: boolean) {
    this.emailNotFound = emailNotFound
    this.generalError = generalError
    this.success = success
  }
}
