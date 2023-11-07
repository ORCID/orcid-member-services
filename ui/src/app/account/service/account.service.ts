import { Injectable } from '@angular/core'
import { SessionStorageService } from 'ngx-webstorage'
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http'
import { BehaviorSubject, EMPTY, Observable, Subject, catchError, map, of, takeUntil, tap } from 'rxjs'

import { SERVER_API_URL } from '../../../app/app.constants'
import { IAccount } from '../model/account.model'
// TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
//import { MSMemberService } from 'app/entities/member/member.service';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private accountData = new BehaviorSubject<IAccount | null | undefined>(undefined)
  private isFetchingAccountData = false
  private stopFetchingAccountData = new Subject()
  private authenticated = false
  private authenticationState = new BehaviorSubject<any>(null)
  private logoutAsResourceUrl = SERVER_API_URL + 'services/userservice/api'

  constructor(
    // TODO: uncomment when language service is implemented
    //private languageService: JhiLanguageService,
    private sessionStorage: SessionStorageService,
    private http: HttpClient // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service //private memberService: MSMemberService
  ) {}

  private fetchAccountData() {
    console.log('Fetching account data from the back end')

    return this.http
      .get<IAccount>('/services/userservice/api/account', {
        observe: 'response',
      })
      .pipe(
        takeUntil(this.stopFetchingAccountData),
        catchError((err) => {
          this.authenticated = false
          this.accountData.next(null)
          // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
          //this.memberService.memberData.next(undefined);
          this.authenticationState.next(this.accountData)
          this.isFetchingAccountData = false
          return EMPTY
        }),
        map((response: HttpResponse<IAccount>) => {
          this.isFetchingAccountData = false
          this.authenticationState.next(this.accountData)
          if (response && response.body) {
            this.authenticated = true
            const account: IAccount = response.body
            this.accountData.next(account)

            // After retrieve the account info, the language will be changed to
            // the user's preferred language configured in the account setting
            if (this.accountData.value?.langKey) {
              const langKey = this.sessionStorage.retrieve('locale') || this.accountData.value.langKey
              // TODO: uncomment when language service is implemented
              //this.languageService.changeLanguage(langKey);
            }
          } else {
            // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
            //this.memberService.memberData.next(undefined);
            this.accountData.next(null)
            this.authenticated = false
            console.error('Invalid response:', response)
          }
        })
      )
  }

  getMfaSetup(): Observable<HttpResponse<any>> {
    return this.http.get<any>('/services/userservice/api/account/mfa', { observe: 'response' })
  }

  save(account: any): Observable<HttpResponse<any>> {
    return this.http.post('/services/userservice/api/account', account, { observe: 'response' })
  }

  enableMfa(mfaSetup: any): Observable<HttpResponse<any>> {
    return this.http.post('/services/userservice/api/account/mfa/on', mfaSetup, { observe: 'response' })
  }

  disableMfa(): Observable<HttpResponse<any>> {
    return this.http.post('/services/userservice/api/account/mfa/off', null, { observe: 'response' })
  }
  // TODO: any - this seems to only be used for logging out (only ever receives null as arg)
  authenticate(identity: any) {
    this.accountData.next(identity)
    this.authenticated = identity !== null
    this.authenticationState.next(this.accountData)
  }

  hasAnyAuthority(authorities: string[]): boolean {
    console.log(authorities, this.accountData.value?.authorities)

    if (!this.authenticated || !this.accountData || !this.accountData.value?.authorities) {
      return false
    }

    for (let i = 0; i < authorities.length; i++) {
      if (this.accountData.value?.authorities.includes(authorities[i])) {
        return true
      }
    }

    return false
  }

  hasAuthority(authority: string): boolean {
    if (!this.authenticated) {
      return false
    } else {
      return this.accountData.value!.authorities && this.accountData.value!.authorities.includes(authority)
    }
  }

  getAccountData(force?: boolean): Observable<IAccount | undefined | null> {
    if (force) {
      // TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
      //this.memberService.stopFetchingMemberData.next();
      //this.memberService.memberData.next(undefined);
      this.stopFetchingAccountData.next(true)
    }
    if ((!this.accountData.value && !this.isFetchingAccountData) || force) {
      this.isFetchingAccountData = true
      this.fetchAccountData().subscribe()
    }

    return this.accountData.asObservable()
  }

  isAuthenticated(): boolean {
    return this.authenticated
  }

  isIdentityResolved(): boolean {
    return this.accountData.value !== undefined
  }

  getImageUrl(): string | null {
    return this.isIdentityResolved() ? this.accountData.value!.imageUrl : null
  }

  getUserName(): string | null {
    let userName: string | null = null

    if (this.isIdentityResolved()) {
      if (this.accountData.value!.firstName) {
        userName = this.accountData.value!.firstName
      }
      if (this.accountData.value!.lastName) {
        if (userName) {
          userName = userName + ' ' + this.accountData.value!.lastName
        } else {
          userName = this.accountData.value!.lastName
        }
      }
      if (userName == null) {
        userName = this.accountData.value!.email
      }
    }
    return userName
  }

  getSalesforceId(): string | null {
    return this.isAuthenticated() && this.accountData ? this.accountData.value!.salesforceId : null
  }

  isOrganizationOwner(): boolean | null {
    return this.isIdentityResolved() && this.accountData ? this.accountData.value!.mainContact : false
  }

  isLoggedAs(): boolean {
    return !!(this.isIdentityResolved() && this.accountData.value && this.accountData.value.loggedAs)
  }

  logoutAs(): Observable<any> {
    const formData = new FormData()
    formData.set('username', this.accountData.value!.loginAs)
    return this.http.post(`${this.logoutAsResourceUrl}/logout_as`, formData, {
      headers: new HttpHeaders().set('Accept', 'text/html'),
      withCredentials: true,
      responseType: 'text',
    })
  }
}
