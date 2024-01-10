export interface IKeyValidationResult {
  expiredKey: boolean
  invalidKey: boolean
}

export class KeyValidationResult implements IKeyValidationResult {
  expiredKey: boolean
  invalidKey: boolean

  constructor(expiredKey: boolean, invalidKey: boolean) {
    this.expiredKey = expiredKey
    this.invalidKey = invalidKey
  }
}
