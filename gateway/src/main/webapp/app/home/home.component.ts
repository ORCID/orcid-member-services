import { Component, OnInit } from '@angular/core';
import { JhiEventManager } from 'ng-jhipster';
import { share } from 'rxjs/operators';
import { AccountService, UserService } from 'app/core';
import { IMSUser } from 'app/shared/model/user.model';
import { MSMemberService } from 'app/entities/member';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  account: IMSUser;

  constructor(private accountService: AccountService, private eventManager: JhiEventManager, private memberService: MSMemberService) {}

  ngOnInit() {
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
      const salesforceId = this.accountService.getSalesforceId();
      console.log(salesforceId);
      this.memberService
        .getMember()
        .pipe()
        .subscribe(res => {
          console.log(res);
        });
    });
    this.registerAuthenticationSuccess();
  }

  registerAuthenticationSuccess() {
    this.eventManager.subscribe('authenticationSuccess', message => {
      this.accountService.identity().then(account => {
        this.account = account;
      });
    });
  }

  isAuthenticated() {
    return this.accountService.isAuthenticated();
  }
}
