import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiLanguageService } from 'ng-jhipster';
import { SessionStorageService } from 'ngx-webstorage';
import { faAddressCard, faUniversity, faChartPie } from '@fortawesome/free-solid-svg-icons';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VERSION } from 'app/app.constants';
import { JhiLanguageHelper, AccountService, LoginModalService, LoginService, Account } from 'app/core';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service.ts';
import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';
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

  faAddressCard = faAddressCard;
  faUniversity = faUniversity;
  faChartPie = faChartPie;

  constructor(
    private loginService: LoginService,
    private languageService: JhiLanguageService,
    private languageHelper: JhiLanguageHelper,
    private sessionStorage: SessionStorageService,
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
    this.languageHelper.getAll().then(languages => {
      this.languages = languages;
    });

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
                this.organizationName = ' | ' + res.body.clientName;
                this.consortiumLead = res.body.isConsortiumLead;
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

  changeLanguage(languageKey: string) {
    this.sessionStorage.store('locale', languageKey);
    this.languageService.changeLanguage(languageKey);
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
