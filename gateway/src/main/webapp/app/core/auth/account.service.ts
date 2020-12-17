import { Injectable } from '@angular/core';
import { JhiLanguageService } from 'ng-jhipster';
import { SessionStorageService } from 'ngx-webstorage';
import { HttpClient, HttpResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { Account } from 'app/core/user/account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private userIdentity: any;
  private authenticated = false;
  private authenticationState = new Subject<any>();
  private logoutAsResourceUrl = SERVER_API_URL + 'services/userservice/api';

  constructor(private languageService: JhiLanguageService, private sessionStorage: SessionStorageService, private http: HttpClient) {}

  fetch(): Observable<HttpResponse<Account>> {
    return this.http.get<Account>(SERVER_API_URL + 'services/userservice/api/account', { observe: 'response' });
  }

  save(account: any): Observable<HttpResponse<any>> {
    return this.http.post(SERVER_API_URL + 'services/userservice/api/account', account, { observe: 'response' });
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

  identity(force?: boolean): Promise<Account> {
    if (force) {
      this.userIdentity = undefined;
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
          // After retrieve the account info, the language will be changed to
          // the user's preferred language configured in the account setting
          if (this.userIdentity.langKey) {
            const langKey = this.sessionStorage.retrieve('locale') || this.userIdentity.langKey;
            this.languageService.changeLanguage(langKey);
          }
        } else {
          this.userIdentity = null;
          this.authenticated = false;
        }
        this.authenticationState.next(this.userIdentity);
        return this.userIdentity;
      })
      .catch(err => {
        this.userIdentity = null;
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
    return this.isIdentityResolved() ? this.userIdentity.salesforceId : null;
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
