import { Component, OnDestroy, OnInit, inject } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { faPencilAlt, faTrashAlt } from '@fortawesome/free-solid-svg-icons'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { EMPTY, Subject, Subscription, combineLatest } from 'rxjs'
import { filter, switchMap, takeUntil } from 'rxjs/operators'
import { AccountService } from 'src/app/account'
import { IAccount } from 'src/app/account/model/account.model'
import { ISFMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { MemberService } from 'src/app/member/service/member.service'

@Component({
  selector: 'app-member-info',
  templateUrl: './member-info.component.html',
  styleUrls: ['member-info.component.scss'],
  standalone: false,
})
export class MemberInfoComponent implements OnInit, OnDestroy {
  private memberService = inject(MemberService)
  private accountService = inject(AccountService)
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  private oidcSecurityService = inject(OidcSecurityService)

  account: IAccount | undefined
  memberData: ISFMemberData | undefined | null
  alertSubscription: Subscription | undefined
  managedMember: string | undefined
  destroy$ = new Subject()
  faTrashAlt = faTrashAlt
  faPencilAlt = faPencilAlt

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
    this.oidcSecurityService.isAuthenticated$
      .pipe(
        filter(({ isAuthenticated }) => isAuthenticated),
        switchMap(() => this.accountService.getAccountData()),
        switchMap((account) => {
          // TypeScript understands this type narrowing perfectly
          if (!account) {
            return EMPTY
          }

          this.account = account

          return this.activatedRoute.params.pipe(
            switchMap((params) => {
              const childId = params['id']

              if (childId) {
                console.log('Fetching member data for managed member ', childId)
                this.managedMember = childId
                this.memberService.setManagedMember(childId)
                return this.memberService.getMemberData(childId)
              } else {
                console.log('Fetching member data for non managed member ', account.memberId)
                this.managedMember = undefined
                this.memberService.setManagedMember(null)
                return this.memberService.getMemberData(account.memberId)
              }
            })
          )
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((data) => {
        this.memberData = data
        // console.log('Member Data successfully loaded:', data)
      })
  }

  stopManagingMember() {
    this.memberService.setManagedMember(null)
    this.memberService.getMemberData(this.account?.memberId, true)
  }

  ngOnDestroy() {
    this.destroy$.next(true)
    this.destroy$.complete()
  }
}
