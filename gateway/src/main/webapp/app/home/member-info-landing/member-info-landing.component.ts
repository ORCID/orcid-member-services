import { Component, OnInit } from '@angular/core';
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

  validateUrl() {
    if (!/(http(s?)):\/\//i.test(this.memberData.website)) {
      this.memberData.website = 'http://' + this.memberData.website;
    }
  }

  ngOnInit() {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
    });
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
    });
    this.accountService.memberData.subscribe(res => {
      if (res) {
        this.memberData = res;
        this.validateUrl();
      }
    });
  }
}
