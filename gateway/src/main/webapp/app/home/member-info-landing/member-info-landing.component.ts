import { Component, OnDestroy, OnInit } from '@angular/core';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-member-info-landing',
  templateUrl: './member-info-landing.component.html',
  styleUrls: ['member-info-landing.component.scss']
})
export class MemberInfoLandingComponent implements OnInit, OnDestroy {
  account: IMSUser;
  memberData: ISFMemberData;
  authenticationStateSubscription: Subscription;
  memberDataSubscription: Subscription;

  constructor(private memberService: MSMemberService, private accountService: AccountService) {}

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
    this.authenticationStateSubscription = this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
    });
    this.memberDataSubscription = this.memberService.memberData.subscribe(res => {
      if (res) {
        this.memberData = res;
        this.validateUrl();
      }
    });
    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
    });
  }

  ngOnDestroy() {
    this.authenticationStateSubscription.unsubscribe();
    this.memberDataSubscription.unsubscribe();
  }
}
