import { Injectable } from '@angular/core';
import { JhiLanguageService } from 'ng-jhipster';
import { SessionStorageService } from 'ngx-webstorage';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { Account } from 'app/core/user/account.model';
import { IMSUser } from 'app/shared/model/user.model';
import { MSMemberService } from 'app/entities/member/member.service';
import { ISFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { SFPublicDetails } from 'app/shared/model/salesforce-public-details.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private userIdentity: any;
  private authenticated = false;
  private authenticationState = new Subject<any>();
  private fetchingMemberDataState = new BehaviorSubject<boolean>(undefined);
  private logoutAsResourceUrl = SERVER_API_URL + 'services/userservice/api';
  memberData: BehaviorSubject<ISFMemberData> = new BehaviorSubject<ISFMemberData>(undefined);

  constructor(
    private languageService: JhiLanguageService,
    private sessionStorage: SessionStorageService,
    private http: HttpClient,
    private memberService: MSMemberService
  ) {}

  fetch(): Observable<HttpResponse<Account>> {
    return this.http.get<Account>(SERVER_API_URL + 'services/userservice/api/account', { observe: 'response' });
  }

  getMfaSetup(): Observable<HttpResponse<any>> {
    return this.http.get<any>(SERVER_API_URL + 'services/userservice/api/account/mfa', { observe: 'response' });
  }

  save(account: any): Observable<HttpResponse<any>> {
    return this.http.post(SERVER_API_URL + 'services/userservice/api/account', account, { observe: 'response' });
  }

  enableMfa(mfaSetup: any): Observable<HttpResponse<any>> {
    return this.http.post(SERVER_API_URL + 'services/userservice/api/account/mfa/on', mfaSetup, { observe: 'response' });
  }

  disableMfa(): Observable<HttpResponse<any>> {
    return this.http.post(SERVER_API_URL + 'services/userservice/api/account/mfa/off', null, { observe: 'response' });
  }

  authenticate(identity) {
    this.userIdentity = identity;
    this.authenticated = identity !== null;
    this.authenticationState.next(this.userIdentity);
  }

  hasAnyAuthority(authorities: string[]): boolean {
    if (!this.authenticated || !this.userIdentity || !this.userIdentity.authorities) {
      return false;
    }

    for (let i = 0; i < authorities.length; i++) {
      if (this.userIdentity.authorities.includes(authorities[i])) {
        return true;
      }
    }

    return false;
  }

  hasAuthority(authority: string): Promise<boolean> {
    if (!this.authenticated) {
      return Promise.resolve(false);
    }

    return this.identity().then(
      id => {
        return Promise.resolve(id.authorities && id.authorities.includes(authority));
      },
      () => {
        return Promise.resolve(false);
      }
    );
  }

  identity(force?: boolean): Promise<IMSUser> {
    if (force) {
      this.userIdentity = undefined;
      this.memberData.next(undefined);
    }

    // check and see if we have retrieved the userIdentity data from the server.
    // if we have, reuse it by immediately resolving
    if (this.userIdentity) {
      return Promise.resolve(this.userIdentity);
    }

    // retrieve the userIdentity data from the server, update the identity object, and then resolve.
    return this.fetch()
      .toPromise()
      .then(response => {
        const account: Account = response.body;
        if (account) {
          this.userIdentity = account;
          this.authenticated = true;
          this.fetchMemberData();
          // After retrieve the account info, the language will be changed to
          // the user's preferred language configured in the account setting
          if (this.userIdentity.langKey) {
            const langKey = this.sessionStorage.retrieve('locale') || this.userIdentity.langKey;
            this.languageService.changeLanguage(langKey);
          }
        } else {
          this.memberData.next(undefined);
          this.userIdentity = null;
          this.authenticated = false;
        }
        this.authenticationState.next(this.userIdentity);
        return this.userIdentity;
      })
      .catch(err => {
        this.userIdentity = null;
        this.memberData.next(undefined);
        this.authenticated = false;
        this.authenticationState.next(this.userIdentity);
        return null;
      });
  }

  isAuthenticated(): boolean {
    return this.authenticated;
  }

  isIdentityResolved(): boolean {
    return this.userIdentity !== undefined;
  }

  getAuthenticationState(): Observable<any> {
    return this.authenticationState.asObservable();
  }

  getImageUrl(): string {
    return this.isIdentityResolved() ? this.userIdentity.imageUrl : null;
  }

  getUserName(): string {
    let userName: string;

    if (this.isIdentityResolved()) {
      if (this.userIdentity.firstName) {
        userName = this.userIdentity.firstName;
      }
      if (this.userIdentity.lastName) {
        if (userName) {
          userName = userName + ' ' + this.userIdentity.lastName;
        } else {
          userName = this.userIdentity.lastName;
        }
      }
      if (userName == null) {
        userName = this.userIdentity.email;
      }
    }
    return userName;
  }

  getSalesforceId(): string {
    return this.isAuthenticated() && this.userIdentity ? this.userIdentity.salesforceId : null;
  }

  updatePublicDetails(data: SFPublicDetails) {
    // TODO: call this.memberData.next(...) instead
    this.memberData.value.publicDisplayName = data.name;
    this.memberData.value.publicDisplayDescriptionHtml = data.description;
    this.memberData.value.website = data.website;
    this.memberData.value.publicDisplayEmail = data.email;
  }

  updateDefaultLanguage(defaultLanguage: string) {
    this.memberData.next({ ...this.memberData.value, defaultLanguage });
  }

  fetchMemberData() {
    if (!this.fetchingMemberDataState.value) {
      if (!this.memberData.value && this.userIdentity) {
        this.fetchingMemberDataState.next(true);
        this.memberService.getMember().subscribe((res: ISFMemberData) => {
          if (res && res.id) {
            // TODO: call this.memberData.next(...) on each update
            this.memberService
              .getMemberContacts()
              .toPromise()
              .then(res => {
                if (res) {
                  this.memberData.value.contacts = res;
                }
              });
            this.memberService
              .getMemberOrgIds()
              .toPromise()
              .then(res => {
                if (res) {
                  this.memberData.value.orgIds = res;
                }
              });
            if (res && res.consortiaLeadId) {
              this.memberService
                .find(res.consortiaLeadId)
                .toPromise()
                .then(r => {
                  if (r && r.body) {
                    this.memberData.value.consortiumLeadName = r.body.clientName;
                  }
                });
            }
            if (this.userIdentity.salesforceId) {
              this.memberService
                .find(this.userIdentity.salesforceId)
                .toPromise()
                .then(r => {
                  if (r && r.body) {
                    this.memberData.value.isConsortiumLead = r.body.isConsortiumLead;
                    this.memberData.value.defaultLanguage = r.body.defaultLanguage;
                  }
                });
            }
            this.memberData.next(res);
          } else {
            this.memberData.next(null);
          }
        });
        this.fetchingMemberDataState.next(false);
      }
    }
  }

  isOrganizationOwner(): string {
    return this.isIdentityResolved() && this.userIdentity ? this.userIdentity.mainContact : false;
  }

  isLoggedAs(): boolean {
    return !!(this.isIdentityResolved() && this.userIdentity && this.userIdentity.loggedAs);
  }

  logoutAs(): Observable<any> {
    const formData = new FormData();
    formData.set('username', this.userIdentity.loginAs);
    return this.http.post(`${this.logoutAsResourceUrl}/logout_as`, formData, {
      headers: new HttpHeaders().set('Accept', 'text/html'),
      withCredentials: true,
      responseType: 'text'
    });
  }
}
