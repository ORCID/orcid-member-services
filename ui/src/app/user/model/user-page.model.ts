import { User } from './user.model'

export interface IUserPage {
  users: User[] | null | undefined
  totalItems: number | null | undefined
}

export class UserPage implements IUserPage {
  constructor(
    public users: User[],
    public totalItems: number
  ) {
    this.users = users
    this.totalItems = totalItems
  }
}
