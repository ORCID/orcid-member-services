import { Member } from './member.model'

export interface IMemberPage {
  members: Member[] | null | undefined
  totalItems: number | null | undefined
}

export class MemberPage implements IMemberPage {
  constructor(
    public members: Member[],
    public totalItems: number
  ) {
    this.members = members
    this.totalItems = totalItems
  }
}
