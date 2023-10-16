import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { faAddressCard, faUniversity, faChartPie, faLightbulb } from '@fortawesome/free-solid-svg-icons';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VERSION } from 'app/app.constants';
import { AccountService, LoginModalService, LoginService, Account } from 'app/core';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { MSMemberService } from 'app/entities/member/member.service';
import { IMSMember } from 'app/shared/model/member.model';
import { SERVER_API_URL } from 'app/app.constants';

type EntityResponseType = HttpResponse<IMSMember>;

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['navbar.scss']
})
export class NavbarComponent implements OnInit {
  inProduction: boolean;
  isNavbarCollapsed: boolean;
  languages: any[];
  swaggerEnabled: boolean;
  modalRef: NgbModalRef;
  version: string;
  member$: Observable<EntityResponseType>;
  organizationName: string;
  memberCallDone: boolean;
  account: Account;
  userName: string;
  consortiumLead: boolean;
  consortiumMember: boolean;

  faAddressCard = faAddressCard;
  faUniversity = faUniversity;
  faChartPie = faChartPie;
  faLightbulb = faLightbulb;

  constructor(
    private loginService: LoginService,
    private accountService: AccountService,
    private loginModalService: LoginModalService,
    private profileService: ProfileService,
    private memberService: MSMemberService,
    private router: Router
  ) {
    this.version = VERSION ? 'v' + VERSION : '';
    this.isNavbarCollapsed = true;
  }

  ngOnInit() {
    this.profileService.getProfileInfo().then(profileInfo => {
      this.inProduction = profileInfo.inProduction;
      this.swaggerEnabled = profileInfo.swaggerEnabled;
    });

    this.accountService.getAuthenticationState().subscribe(() => {
      if (!this.isAuthenticated()) {
        return null;
      } else if (!this.accountService.getSalesforceId()) {
        return null;
      }
      if (!this.memberCallDone && this.isAuthenticated() && this.hasRoleUser()) {
        this.memberCallDone = true;
        this.memberService
          .find(this.accountService.getSalesforceId())
          .toPromise()
          .then(
            (res: HttpResponse<IMSMember>) => {
              if (res.body) {
                this.organizationName = res.body.clientName;
                this.consortiumLead = res.body.isConsortiumLead;
                this.consortiumMember = res.body.parentSalesforceId != null;
              }
              return this.organizationName;
            },
            (res: HttpErrorResponse) => {
              console.error('Error when getting org name: ' + res.error);
              return null;
            }
          );
      }
    });
  }

  collapseNavbar() {
    this.isNavbarCollapsed = true;
  }

  isAuthenticated() {
    return this.accountService.isAuthenticated();
  }

  isOrganizationOwner() {
    return this.accountService.isOrganizationOwner();
  }

  hasRoleUser() {
    return this.accountService.hasAnyAuthority(['ROLE_USER']);
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN']);
  }

  isLoggedAs() {
    return this.accountService.isLoggedAs();
  }

  getUserName() {
    // return this.isAuthenticated() ? this.userName : null;
    return this.isAuthenticated() ? this.accountService.getUserName() : null;
  }

  login() {
    this.modalRef = this.loginModalService.open();
  }

  logout() {
    this.collapseNavbar();
    this.organizationName = null;
    this.memberCallDone = false;
    this.userName = null;
    if (this.isLoggedAs()) {
      this.accountService.logoutAs().subscribe(() => {
        window.location.href = SERVER_API_URL;
      });
    } else {
      this.memberService.setManagedMember(null);
      this.loginService.logout();
      this.router.navigate(['']);
    }
  }

  logoutAs() {
    this.collapseNavbar();
    this.organizationName = null;
    this.memberCallDone = false;
    this.userName = null;
    this.accountService.logoutAs().subscribe(res => {
      window.location.href = SERVER_API_URL;
    });
  }

  toggleNavbar() {
    this.isNavbarCollapsed = !this.isNavbarCollapsed;
  }

  getImageUrl() {
    return this.isAuthenticated() ? this.accountService.getImageUrl() : null;
  }
}
