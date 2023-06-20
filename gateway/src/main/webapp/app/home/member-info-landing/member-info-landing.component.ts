import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subject, Subscription } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';

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
  alertSubscription: Subscription;
  managedMember: string;
  destroy$ = new Subject();
  constructor(
    private memberService: MSMemberService,
    private accountService: AccountService,
    private location: Location,
    protected activatedRoute: ActivatedRoute,
    protected router: Router
  ) {}

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
    this.activatedRoute.params.subscribe(params => {
      if (params['id']) {
        this.managedMember = params['id'];
        this.memberService.setManagedMember(params['id']);
      }
    });
    this.authenticationStateSubscription = this.accountService
      // get account info
      .getAuthenticationState()
      .pipe(
        tap(account => (this.account = account)),
        switchMap(() => this.memberService.memberData),
        takeUntil(this.destroy$)
      )
      // fetch member data
      .subscribe(memberData => {
        // fetch managed member data if we've started managing a member
        if (this.managedMember && this.account.salesforceId === memberData.id) {
          this.memberService.fetchMemberData(this.managedMember);
        }

        this.memberData = memberData;
      });

    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
    });
  }

  stopManagingMember() {
    // empty string if it's not working
    this.memberService.setManagedMember(null);
    this.memberService.fetchMemberData(this.account.salesforceId);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.authenticationStateSubscription) {
      this.authenticationStateSubscription.unsubscribe();
    }
    if (this.memberDataSubscription) {
      this.memberDataSubscription.unsubscribe();
    }
  }
}
