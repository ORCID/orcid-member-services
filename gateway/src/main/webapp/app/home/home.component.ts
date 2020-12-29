import { Component, OnInit } from '@angular/core';
import { JhiEventManager } from 'ng-jhipster';

import { AccountService } from 'app/core';
import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';

@Component({
  selector: 'jhi-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  account: IMSUser;

  constructor(private accountService: AccountService, private eventManager: JhiEventManager) {}

  ngOnInit() {
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
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
