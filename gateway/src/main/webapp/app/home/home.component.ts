import { Component, OnInit } from '@angular/core';
import { JhiEventManager } from 'ng-jhipster';
import { AccountService } from 'app/core';
import { IMSUser } from 'app/shared/model/user.model';
import { ISFMemberData } from 'app/shared/model/salesforce.member.data.model';
import { MSMemberService } from 'app/entities/member';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  account: IMSUser;
  memberData: ISFMemberData;

  constructor(private accountService: AccountService, private eventManager: JhiEventManager, private memberService: MSMemberService) {}

  ngOnInit() {
    this.initializeAccount();
    this.eventManager.subscribe('authenticationSuccess', message => {
      this.initializeAccount();
    });
  }

  initializeAccount() {
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
      this.memberService
        .getMember()
        .pipe()
        .subscribe(res => {
          this.memberData = res;
        });
    });
  }

  isAuthenticated() {
    return this.accountService.isAuthenticated();
  }
}
