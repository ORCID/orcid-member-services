import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Router, RouterLink, RouterLinkActive } from '@angular/router'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import {
  faAddressCard,
  faBars,
  faChartPie,
  faKey,
  faLightbulb,
  faLock,
  faSignOutAlt,
  faUniversity,
  faUser,
  faUserPlus,
  faWrench,
} from '@fortawesome/free-solid-svg-icons'
import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { NgbDropdown, NgbDropdownMenu, NgbDropdownToggle } from '@ng-bootstrap/ng-bootstrap/dropdown'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { of } from 'rxjs'
import { IMember } from 'src/app/member/model/member.model'
import { MemberService } from 'src/app/member/service/member.service'
import { FeatureToggleService } from 'src/app/shared/service/feature-toggle.service'
import { AccountService, LoginService } from '../../account'
import { ApiCredentialsMfaEnabledDialogComponent } from './api-credentials-mfa-enabled-dialog/api-credentials-mfa-enabled-dialog.component'

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    FaIconComponent,
    NgbDropdown,
    RouterLinkActive,
    NgbDropdownToggle,
    NgbDropdownMenu,
  ],
})
export class NavbarComponent {
  private loginService = inject(LoginService)
  private accountService = inject(AccountService)
  private memberService = inject(MemberService)
  private oidcSecurityService = inject(OidcSecurityService)
  protected featureService = inject(FeatureToggleService)
  private router = inject(Router)
  private modalService = inject(NgbModal)
  private destroyRef = inject(DestroyRef)

  protected isNavbarCollapsed = signal(true)
  protected organizationName = signal<string | undefined>(undefined)
  protected consortiumLead = signal<boolean>(false)
  protected consortiumMember = signal<boolean>(false)
  protected memberCallDone = signal(false)
  protected isUserAuthenticated = signal(false)
  protected userHasRoleUser = signal(false)
  protected userHasRoleAdmin = signal(false)
  protected userHasAssertionService = signal(false)
  protected userIsOrgOwner = signal(false)
  protected userIsMFAEnabled = signal(false)
  protected userCanManageApiCreds = signal(false)
  protected userImageUrl = signal<string | null>(null)

  protected canShowAdmin = computed(() => this.userIsOrgOwner() || this.userHasRoleAdmin())
  protected hasUserImage = computed(() => !!this.userImageUrl())

  protected faBars = faBars
  protected faUser = faUser
  protected faUserPlus = faUserPlus
  protected faSignOutAlt = faSignOutAlt
  protected faWrench = faWrench
  protected faLock = faLock
  protected faAddressCard = faAddressCard
  protected faUniversity = faUniversity
  protected faChartPie = faChartPie
  protected faLightbulb = faLightbulb
  protected faKey = faKey

  constructor() {
    const oidcAuthState$ = (this.oidcSecurityService as any).isAuthenticated$ ?? of({ isAuthenticated: false })
    const accountData$ = this.accountService.getAccountData() ?? of(null)

    oidcAuthState$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((authState: any) => {
        const isAuthenticated = !!authState?.isAuthenticated
        this.isUserAuthenticated.set(isAuthenticated)
        if (isAuthenticated) {
          this.updateAuthenticationState()
          this.loadMemberData()
        } else {
          this.resetAuthenticationState()
        }
      })

    accountData$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((account) => {
        if (account) {
          this.updateAuthenticationState()
        }
      })
  }

  private updateAuthenticationState() {
    this.userHasRoleUser.set(this.accountService.hasAnyAuthority(['ROLE_USER']))
    this.userHasRoleAdmin.set(this.accountService.hasAnyAuthority(['ROLE_ADMIN']))
    this.userHasAssertionService.set(this.accountService.hasAnyAuthority(['ASSERTION_SERVICE_ENABLED']))
    this.userIsOrgOwner.set(this.accountService.isOrganizationOwner() || false)
    this.userIsMFAEnabled.set(this.accountService.isMFAEnabled() || false)
    this.userCanManageApiCreds.set(this.accountService.isManageApiCredentialsEnabled() || false)
    this.userImageUrl.set(this.accountService.getImageUrl())
  }

  private loadMemberData() {
    if (this.memberCallDone() || !this.userHasRoleUser()) {
      return
    }

    this.memberCallDone.set(true)
    const memberId = this.accountService.getMemberId()

    if (memberId) {
      this.memberService.find(memberId).subscribe({
        next: (res: IMember | null) => {
          if (res) {
            this.organizationName.set(res.clientName)
            this.consortiumLead.set(res.isConsortiumLead || false)
            this.consortiumMember.set(res.parentSalesforceId != null)
          }
        },
        error: (err) => console.error('Member fetch failed', err),
      })
    } else {
      console.warn('Authenticated, but Salesforce ID is missing!')
    }

    this.featureService.initFeatures().subscribe()
  }

  private resetAuthenticationState() {
    this.organizationName.set(undefined)
    this.consortiumLead.set(false)
    this.consortiumMember.set(false)
    this.memberCallDone.set(false)
    this.userHasRoleUser.set(false)
    this.userHasRoleAdmin.set(false)
    this.userHasAssertionService.set(false)
    this.userIsOrgOwner.set(false)
    this.userIsMFAEnabled.set(false)
    this.userCanManageApiCreds.set(false)
    this.userImageUrl.set(null)
  }

  collapseNavbar() {
    this.isNavbarCollapsed.set(true)
  }

  toggleNavbar() {
    this.isNavbarCollapsed.update((collapsed) => !collapsed)
  }

  isAuthenticated() {
    return this.isUserAuthenticated()
  }

  isOrganizationOwner() {
    return this.userIsOrgOwner()
  }

  isManageApiCredentialsEnabled() {
    return this.userCanManageApiCreds()
  }

  isMFAEnabled() {
    return this.userIsMFAEnabled()
  }

  hasRoleUser() {
    return this.userHasRoleUser()
  }

  hasRoleAdmin() {
    return this.userHasRoleAdmin()
  }

  hasAssertionServiceEnabled() {
    return this.userHasAssertionService()
  }

  getImageUrl() {
    return this.userImageUrl()
  }

  logout() {
    this.collapseNavbar()
    this.memberService.setManagedMember(null)
    this.loginService.logout()
  }

  manageApiCredentials() {
    if (this.userIsMFAEnabled()) {
      this.collapseNavbar()
      this.router.navigate(['/api-credentials'])
    } else {
      this.modalService.open(ApiCredentialsMfaEnabledDialogComponent, {
        backdrop: 'static',
        centered: true,
      })
    }
  }

}
