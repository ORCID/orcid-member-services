import { Component, OnInit } from '@angular/core'
import {
  faAddressCard,
  faUniversity,
  faChartPie,
  faLightbulb,
  faBars,
  faUser,
  faUserPlus,
  faSignOutAlt,
  faWrench,
  faLock,
} from '@fortawesome/free-solid-svg-icons'
import { AccountService, LoginService } from '../../account'
import { MemberService } from 'src/app/member/service/member.service'
import { IAccount } from 'src/app/account/model/account.model'
import { IMember } from 'src/app/member/model/member.model'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { filter, switchMap } from 'rxjs/operators'

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.scss'],
})
export class NavbarComponent implements OnInit {
  isNavbarCollapsed: boolean

  organizationName: string | undefined
  account: IAccount | undefined
  username: string | undefined
  consortiumLead: boolean | undefined
  memberCallDone = false
  consortiumMember = false

  faBars = faBars
  faUser = faUser
  faUserPlus = faUserPlus
  faSignOutAlt = faSignOutAlt
  faWrench = faWrench
  faLock = faLock
  faAddressCard = faAddressCard
  faUniversity = faUniversity
  faChartPie = faChartPie
  faLightbulb = faLightbulb

  constructor(
    private loginService: LoginService,
    private accountService: AccountService,
    private memberService: MemberService,
    private oidcSecurityService: OidcSecurityService
  ) {
    this.isNavbarCollapsed = true
  }

  ngOnInit() {
    this.oidcSecurityService.isAuthenticated$
      .pipe(
        // 1. Wait until the OIDC library confirms we are logged in
        filter(({ isAuthenticated }) => isAuthenticated),

        // 2. Once logged in, switch to waiting for the Account Data
        // (This ensures we don't use a stale/cached ID before the new token is ready)
        switchMap(() => this.accountService.getAccountData())
      )
      .subscribe(() => {
        // 3. Now we are safe: We have a Token AND User Data
        if (!this.memberCallDone && this.hasRoleUser()) {
          this.memberCallDone = true

          const salesforceId = this.accountService.getSalesforceId()

          if (salesforceId) {
            console.log('Fetching member for ID:', salesforceId)
            this.memberService.find(salesforceId).subscribe({
              next: (res: IMember | null) => {
                if (res) {
                  this.organizationName = res.clientName
                  this.consortiumLead = res.isConsortiumLead
                  this.consortiumMember = res.parentSalesforceId != null
                }
              },
              error: (err) => console.error('Member fetch failed', err),
            })
          } else {
            console.warn('Authenticated, but Salesforce ID is missing!')
          }
        }
      })
  }

  collapseNavbar() {
    this.isNavbarCollapsed = true
  }

  isAuthenticated() {
    return this.accountService.isAuthenticated()
  }

  isOrganizationOwner() {
    return this.accountService.isOrganizationOwner()
  }

  hasRoleUser() {
    return this.accountService.hasAnyAuthority(['ROLE_USER'])
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN'])
  }

  logout() {
    this.collapseNavbar()
    this.organizationName = undefined
    this.memberCallDone = false
    this.username = undefined
    this.memberService.setManagedMember(null)
    this.loginService.logout()
  }

  toggleNavbar() {
    this.isNavbarCollapsed = !this.isNavbarCollapsed
  }

  getImageUrl() {
    return this.isAuthenticated() ? this.accountService.getImageUrl() : null
  }
}
