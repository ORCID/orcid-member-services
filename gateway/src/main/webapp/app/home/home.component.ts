import { Component, OnDestroy, OnInit } from '@angular/core';
import { AccountService } from 'app/core';
import { IMSUser } from 'app/shared/model/user.model';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { Subscription } from 'rxjs';

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

  constructor(private accountService: AccountService) {}

  ngOnInit() {
    this.authenticationStateSubscription = this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      this.accountService.fetchMemberData();
    });
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
      this.accountService.fetchMemberData();
    });
    this.memberDataSubscription = this.accountService.memberData.subscribe(data => {
      this.memberData = data;
    });
  }

  ngOnDestroy() {
    this.authenticationStateSubscription.unsubscribe();
    this.memberDataSubscription.unsubscribe();
  }
}
