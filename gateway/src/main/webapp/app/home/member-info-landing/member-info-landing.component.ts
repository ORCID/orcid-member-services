import { Component, Input, OnInit } from '@angular/core';
import { AccountService } from 'app/core';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { IMSUser } from 'app/shared/model/user.model';

@Component({
  selector: 'app-member-info-landing',
  templateUrl: './member-info-landing.component.html',
  styleUrls: ['member-info-landing.component.scss']
})
export class MemberInfoLandingComponent implements OnInit {
  account: IMSUser;
  memberData: ISFMemberData;
  fetchingMemberData: boolean = undefined;

  constructor(private accountService: AccountService) {}

  isActive() {
    return this.memberData && new Date(this.memberData.membershipEndDateString) > new Date();
  }

  filterCRFID(id) {
    return id.replace(/^.*dx.doi.org\//g, '');
  }

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
      console.log('a');
    } else if (this.account !== null && !this.memberData) {
      console.log('a');
      this.accountService.getCurrentMemberData().then(res => {
        console.log(res);
        this.memberData = res.value;
      });
    }
  }
}
