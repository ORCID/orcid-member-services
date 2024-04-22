import { Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { faPencilAlt, faTrashAlt } from '@fortawesome/free-solid-svg-icons'
import { EMPTY, Subject, Subscription, combineLatest } from 'rxjs'
import { switchMap, takeUntil } from 'rxjs/operators'
import { AccountService } from 'src/app/account'
import { IAccount } from 'src/app/account/model/account.model'
import { ISFMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { MemberService } from 'src/app/member/service/member.service'

@Component({
  selector: 'app-member-info',
  templateUrl: './member-info.component.html',
  styleUrls: ['member-info.component.scss'],
})
export class MemberInfoComponent implements OnInit, OnDestroy {
  account: IAccount | undefined
  memberData: ISFMemberData | undefined | null
  alertSubscription: Subscription | undefined
  managedMember: string | undefined
  destroy$ = new Subject()
  faTrashAlt = faTrashAlt
  faPencilAlt = faPencilAlt
  constructor(
    private memberService: MemberService,
    private accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router
  ) {}

  isActive() {
    return this.memberData?.membershipEndDateString && new Date(this.memberData.membershipEndDateString) > new Date()
  }

  filterCRFID(id: string) {
    return id.replace(/^.*dx.doi.org\//g, '')
  }

  validateUrl() {
    if (this.memberData?.website && !/(http(s?)):\/\//i.test(this.memberData.website)) {
      this.memberData.website = 'http://' + this.memberData.website
    }
  }

  ngOnInit() {
    combineLatest([this.activatedRoute.params, this.accountService.getAccountData()])
      .pipe(
        switchMap(([params, account]) => {
          if (params['id']) {
            this.managedMember = params['id']
          }

          if (account) {
            this.account = account
            if (this.managedMember) {
              this.memberService.setManagedMember(params['id'])
              return this.memberService.getMemberData(this.managedMember)
            } else {
              return this.memberService.getMemberData(account?.salesforceId)
            }
          } else {
            return EMPTY
          }
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((data) => {
        this.memberData = data
      })
  }

  stopManagingMember() {
    this.memberService.setManagedMember(null)
    this.memberService.getMemberData(this.account?.salesforceId, true)
  }

  ngOnDestroy() {
    this.destroy$.next(true)
    this.destroy$.complete()
  }
}
