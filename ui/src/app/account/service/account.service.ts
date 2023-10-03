import { Injectable } from '@angular/core';
import { SessionStorageService } from 'ngx-webstorage';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';

import { SERVER_API_URL } from '../../../app/app.constants';
import { IAccount } from '../model/account.model';
// TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
//import { MSMemberService } from 'app/entities/member/member.service';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private userIdentity: any;
  private authenticated = false;
  private authenticationState = new BehaviorSubject<any>(null);
  private logoutAsResourceUrl = SERVER_API_URL + 'services/userservice/api';

  constructor(
    // TODO: uncomment when language service is implemented
    //private languageService: JhiLanguageService,
    private sessionStorage: SessionStorageService,
    private http: HttpClient,
    // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
    //private memberService: MSMemberService
  ) { }

  fetch(): Observable<HttpResponse<IAccount>> {
    return this.http.get<IAccount>(SERVER_API_URL + 'services/userservice/api/account', { observe: 'response' });
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
  // TODO: any - this seems to only be used for logging out (only ever receives null as arg)
  authenticate(identity: any) {
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

  identity(force?: boolean): Promise<IAccount> {
    if (force) {
      // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service

      //this.memberService.stopFetchingMemberData.next();
      this.userIdentity = undefined;
      //this.memberService.memberData.next(undefined);
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
        // TODO: change into an observable
        // since a promise has to return something it can return null
      // whereas an observable will only return something when it's available therefore it will not be null or undefined
        if (response && response.body) {
          const account: IAccount = response.body;
          this.userIdentity = account;
          this.authenticated = true;
          // After retrieve the account info, the language will be changed to
          // the user's preferred language configured in the account setting
          if (this.userIdentity.langKey) {
            const langKey = this.sessionStorage.retrieve('locale') || this.userIdentity.langKey;
            // TODO: uncomment when language service is implemented
            //this.languageService.changeLanguage(langKey);
          }

        } else {
          // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
          //this.memberService.memberData.next(undefined);
          this.userIdentity = null;
          this.authenticated = false;
        }
        this.authenticationState.next(this.userIdentity);
        return this.userIdentity;
      })
      .catch(err => {
        this.userIdentity = null;
        // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
        //this.memberService.memberData.next(undefined);
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

  getUserName(): string | null {
    let userName: string | null = null;

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
