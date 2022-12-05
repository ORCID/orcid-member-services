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
  fetchingMemberData: boolean = undefined;
  MEMBER_LIST_URL: string = 'https://orcid.org/members';

  constructor(private accountService: AccountService) {}
  ngOnInit() {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      this.getMemberData();
    });
    // TODO add fetchingMemberData check to the account service
    this.accountService.identity().then((account: IMSUser) => {
      if (!this.fetchingMemberData) {
        this.account = account;
        this.getMemberData();
      }
    });
  }

  filterCRFID(id) {
    return id.replace(/^.*dx.doi.org\//g, '');
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
