import { Component, OnDestroy, OnInit } from '@angular/core'
import { AccountService } from '../account'
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
  salesforceId: string | undefined
  loggedInMessage: string | undefined
  accountServiceSubscription: Subscription | undefined

  constructor(private accountService: AccountService) {}

  ngOnInit() {
    this.accountServiceSubscription = this.accountService.getAccountData().subscribe((account) => {
      this.account = account
      if (account) {
        this.loggedInMessage = $localize`:@@home.loggedIn.message.string:You are logged in as user <strong>${account.email}</strong>`
      }
    })
  }

  ngOnDestroy(): void {
    if (this.accountServiceSubscription) {
      this.accountServiceSubscription.unsubscribe()
    }
  }
}
