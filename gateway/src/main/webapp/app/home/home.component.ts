import { Component, OnDestroy, OnInit } from '@angular/core';
import { AccountService } from 'app/core';
import { IMSUser } from 'app/shared/model/user.model';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { Subscription } from 'rxjs';
import { MSMemberService } from 'app/entities/member';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  account: IMSUser;
  memberData: ISFMemberData;
  authenticationStateSubscription: Subscription;
  memberDataSubscription: Subscription;
  manageMemberSubscription: Subscription;
  salesforceId: string;
  manage: string;

  constructor(private accountService: AccountService, private memberService: MSMemberService) {}

  ngOnInit() {
    this.manageMemberSubscription = this.memberService.getManagedMember().subscribe(manage => {
      this.manage = manage;
    });

    this.authenticationStateSubscription = this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      if (account) {
        this.memberService.fetchMemberData(account.salesforceId);
      }
    });
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
      if (account) {
        this.memberService.fetchMemberData(account.salesforceId);
      }
    });
    this.memberDataSubscription = this.memberService.memberData.subscribe(data => {
      this.memberData = data;
    });
  }

  ngOnDestroy() {
    if (this.authenticationStateSubscription) {
      this.authenticationStateSubscription.unsubscribe();
    }
    if (this.memberDataSubscription) {
      this.memberDataSubscription.unsubscribe();
    }
    if (this.manageMemberSubscription) {
      this.manageMemberSubscription.unsubscribe();
    }
  }
}
