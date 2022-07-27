import { Component, ComponentFactoryResolver, OnInit } from '@angular/core';
import { JhiEventManager } from 'ng-jhipster';
import { AccountService } from 'app/core';
import { IMSUser } from 'app/shared/model/user.model';
import { ISFMemberData } from 'app/shared/model/salesforce.member.data.model';
import { MSMemberService } from 'app/entities/member';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  account: IMSUser;
  memberData: ISFMemberData;
  memberDataLoaded: boolean;

  constructor(private accountService: AccountService, private memberService: MSMemberService) {}

  ngOnInit() {
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
    });
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      if (account === null) {
        this.memberData = null;
      } else {
        this.memberService
          .getMember()
          .pipe()
          .subscribe(res => {
            this.memberData = res;
          });
      }
    });
  }
}
