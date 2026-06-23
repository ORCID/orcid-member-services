import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { faPencilAlt, faTrashAlt } from '@fortawesome/free-solid-svg-icons'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { EMPTY } from 'rxjs'
import { filter, switchMap } from 'rxjs/operators'
import { AccountService } from 'src/app/account'
import { IAccount } from 'src/app/account/model/account.model'
import { ISFMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { MemberService } from 'src/app/member/service/member.service'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-member-info',
  templateUrl: './member-info.component.html',
  styleUrls: ['member-info.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FaIconComponent],
})
export class MemberInfoComponent implements OnInit {
  private memberService = inject(MemberService)
  private accountService = inject(AccountService)
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  private oidcSecurityService = inject(OidcSecurityService)
  private destroyRef = inject(DestroyRef)

  protected account = signal<IAccount | undefined>(undefined)
  protected memberData = signal<ISFMemberData | undefined | null>(null)
  protected managedMember = signal<string | undefined>(undefined)
  protected faTrashAlt = faTrashAlt
  protected faPencilAlt = faPencilAlt

  isActive() {
    const membershipEndDate = this.memberData()?.membershipEndDateString
    return !!membershipEndDate && new Date(membershipEndDate) > new Date()
  }

  filterCRFID(id: string) {
    return id.replace(/^.*dx.doi.org\//g, '')
  }

  validateUrl() {
    const memberData = this.memberData()
    if (memberData?.website && !/(http(s?)):\/\//i.test(memberData.website)) {
      this.memberData.set({ ...memberData, website: 'http://' + memberData.website })
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

          this.account.set(account)

          return this.activatedRoute.params.pipe(
            switchMap((params) => {
              const childId = params['id']

              if (childId) {
                this.managedMember.set(childId)
                this.memberService.setManagedMember(childId)
                return this.memberService.getMemberData(childId)
              } else {
                this.managedMember.set(undefined)
                this.memberService.setManagedMember(null)
                return this.memberService.getMemberData(account.memberId)
              }
            })
          )
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((data) => {
        this.memberData.set(data)
      })
  }

  stopManagingMember() {
    this.memberService.setManagedMember(null)
    this.memberService.getMemberData(this.account()?.memberId, true)
  }
}
