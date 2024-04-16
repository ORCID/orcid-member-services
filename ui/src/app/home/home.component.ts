import { Component, OnDestroy, OnInit } from '@angular/core'
import { AccountService } from '../account'
import { MemberService } from '../member/service/member.service'
import { Subscription } from 'rxjs/internal/Subscription'
import { ISFMemberData } from '../member/model/salesforce-member-data.model'
import { IAccount } from '../account/model/account.model'

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit, OnDestroy {
  account: IAccount | undefined | null
  memberData: ISFMemberData | undefined | null
  authenticationStateSubscription: Subscription | undefined
  memberDataSubscription: Subscription | undefined
  manageMemberSubscription: Subscription | undefined
  salesforceId: string | undefined
  loggedInMessage: string | undefined

  constructor(
    private accountService: AccountService,
    private memberService: MemberService
  ) {}

  ngOnInit() {
    this.accountService.getAccountData().subscribe((account) => {
      this.account = account
      if (account) {
        this.memberService.fetchMemberData(account.salesforceId)
        this.loggedInMessage = $localize`:@@home.loggedIn.message.string:You are logged in as user ${account.email}`
      }
    })
    this.memberDataSubscription = this.memberService.memberData.subscribe((data) => {
      this.memberData = data
    })
  }

  ngOnDestroy() {
    if (this.authenticationStateSubscription) {
      this.authenticationStateSubscription.unsubscribe()
    }
    if (this.memberDataSubscription) {
      this.memberDataSubscription.unsubscribe()
    }
    if (this.manageMemberSubscription) {
      this.manageMemberSubscription.unsubscribe()
    }
  }
}
