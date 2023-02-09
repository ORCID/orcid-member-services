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

  constructor(private accountService: AccountService) {}

  ngOnInit() {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
    });
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
    });
    this.accountService.memberData.subscribe(data => {
      this.memberData = data;
    });
  }
}
