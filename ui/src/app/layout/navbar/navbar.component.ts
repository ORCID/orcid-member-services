import { Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
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
import { HttpResponse, HttpErrorResponse } from '@angular/common/http'
import { AccountService, LoginService } from '../../account'
import { MemberService } from 'src/app/member/service/member.service'
import { IAccount } from 'src/app/account/model/account.model'
import { IMember } from 'src/app/member/model/member.model'

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.scss'],
})
export class NavbarComponent implements OnInit {
  isNavbarCollapsed: boolean

  organizationName: string | undefined
  account: IAccount | undefined
  userName: string | undefined
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
    private router: Router
  ) {
    this.isNavbarCollapsed = true
  }

  ngOnInit() {
    this.accountService.getAuthenticationState().subscribe(() => {
      if (!this.memberCallDone && this.isAuthenticated() && this.hasRoleUser()) {
        this.memberCallDone = true

        const salesforceId = this.accountService.getSalesforceId()
        if (salesforceId) {
          this.memberService.find(salesforceId).subscribe({
            next: (res: IMember | null) => {
              if (res) {
                this.organizationName = res.clientName
                this.consortiumLead = res.isConsortiumLead
                this.consortiumMember = res.parentSalesforceId != null
              }
              return this.organizationName
            },
            error: (res: HttpErrorResponse) => {
              console.error('Error when getting org name: ' + res.error)
              return null
            },
          })
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

  isLoggedAs() {
    return this.accountService.isLoggedAs()
  }

  getUserName() {
    // return this.isAuthenticated() ? this.userName : null;
    return this.isAuthenticated() ? this.accountService.getUserName() : null
  }

  logout() {
    this.collapseNavbar()
    this.organizationName = undefined
    this.memberCallDone = false
    this.userName = undefined
    if (this.isLoggedAs()) {
      this.accountService.logoutAs().subscribe(() => {
        window.location.href = '/'
      })
    } else {
      this.memberService.setManagedMember(null)
      this.loginService.logout()
      console.log('logged out now')
      this.router.navigate(['/login'])
    }
  }

  logoutAs() {
    this.collapseNavbar()
    this.organizationName = undefined
    this.memberCallDone = false
    this.userName = undefined
    this.accountService.logoutAs().subscribe((res) => {
      window.location.href = '/'
    })
  }

  toggleNavbar() {
    this.isNavbarCollapsed = !this.isNavbarCollapsed
  }

  getImageUrl() {
    return this.isAuthenticated() ? this.accountService.getImageUrl() : null
  }
}
