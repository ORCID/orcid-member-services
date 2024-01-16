export interface IUserValidation {
  valid: boolean
  errors: string[] | undefined | null
}

export class UserValidation implements IUserValidation {
  constructor(
    public valid: boolean,
    public errors: string[] | undefined | null
  ) {
    this.valid = valid || false
    this.errors = errors
  }
}
