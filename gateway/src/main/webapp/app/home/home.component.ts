import { Component, OnInit } from '@angular/core';
import { AccountService } from 'app/core';
import { IMSUser } from 'app/shared/model/user.model';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  account: IMSUser;
  memberData: ISFMemberData;
  memberDataLoaded: boolean = false;

  constructor(private accountService: AccountService) {}

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
      this.memberData = undefined;
      this.memberDataLoaded = false;
    } else if (this.account !== null && !this.memberData) {
      this.accountService.getCurrentMemberData().then(res => {
        this.memberDataLoaded = true;
        if (res && res.value) {
          this.memberData = res.value;
        } else if (res && res.value === null) {
          this.memberData = null;
        }
      });
    }
  }
}
