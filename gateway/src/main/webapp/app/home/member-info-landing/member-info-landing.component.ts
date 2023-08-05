import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/member';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { IMSUser } from 'app/shared/model/user.model';
import { Subject, Subscription, combineLatest } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-member-info-landing',
  templateUrl: './member-info-landing.component.html',
  styleUrls: ['member-info-landing.component.scss']
})
export class MemberInfoLandingComponent implements OnInit, OnDestroy {
  account: IMSUser;
  memberData: ISFMemberData;
  memberDataSubscription: Subscription;
  alertSubscription: Subscription;
  managedMember: string;
  destroy$ = new Subject();
  constructor(
    private memberService: MSMemberService,
    private accountService: AccountService,
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
    if (this.memberData.website && !/(http(s?)):\/\//i.test(this.memberData.website)) {
      this.memberData.website = 'http://' + this.memberData.website;
    }
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe(params => {
      if (params['id']) {
        this.managedMember = params['id'];
        this.memberService.setActiveMember(params['id'], true);
      }
    });
    combineLatest([this.memberService.memberData, this.accountService.getAuthenticationState()])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([memberData, account]) => {
        console.log('skldjfhskdjfhskdjfhskdjfhskdjfhskdjfhskdjfh');

        this.account = account;
        this.memberData = memberData;
      });

    this.accountService.identity().then((account: IMSUser) => {
      this.account = account;
    });
  }

  stopManagingMember() {
    this.memberService.setActiveMember(this.account.salesforceId, false);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.memberDataSubscription) {
      this.memberDataSubscription.unsubscribe();
    }
  }
}
