import { Component, Input, OnInit } from '@angular/core';
import { AccountService } from 'app/core';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { IMSUser } from 'app/shared/model/user.model';

@Component({
  selector: 'app-member-info-edit',
  templateUrl: './member-info-edit.component.html',
  styleUrls: ['./member-info-edit.component.scss']
})
export class MemberInfoEditComponent implements OnInit {
  account: IMSUser;
  memberData: ISFMemberData;
  // memberDataLoaded = false;
  fetchingMemberData: boolean = undefined;

  constructor(private accountService: AccountService) {}
  ngOnInit() {
    this.accountService.getFetchingMemberDataState().subscribe(fetchingMemberData => {
      this.fetchingMemberData = fetchingMemberData;
    });
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      this.getMemberData();
    });
    this.accountService.identity().then((account: IMSUser) => {
      if (!this.fetchingMemberData) {
        this.account = account;
        this.getMemberData();
      }
    });
  }

  getMemberData() {
    if (this.account === null) {
      this.memberData = null;
    } else if (this.account !== null && !this.memberData) {
      this.accountService.getCurrentMemberData().then(res => {
        this.memberData = res.value;
      });
    }
  }
}
