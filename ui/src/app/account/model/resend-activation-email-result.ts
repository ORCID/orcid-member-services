export interface IResendActivationEmailResult {
  resent: boolean
}

export class ResendActivationEmailResult implements IResendActivationEmailResult {
  resent: boolean

  constructor(resent: boolean) {
    this.resent = resent
  }
}
