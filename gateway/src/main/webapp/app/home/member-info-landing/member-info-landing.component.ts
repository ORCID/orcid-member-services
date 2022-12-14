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

  constructor(private accountService: AccountService) {}

  isActive() {
    return this.memberData && new Date(this.memberData.membershipEndDateString) > new Date();
  }

  filterCRFID(id) {
    return id.replace(/^.*dx.doi.org\//g, '');
  }

  ngOnInit() {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      this.getMemberData();
    });
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
      this.getMemberData();
    });
  }

  getMemberData() {
    if (this.account === null) {
      this.memberData = null;
    } else if (this.account !== null && !this.memberData) {
      this.accountService.getCurrentMemberData().then(res => {
        if (res && res.value) {
          this.memberData = res.value;
        }
      });
    }
  }
}
