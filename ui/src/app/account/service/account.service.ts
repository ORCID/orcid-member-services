import { Injectable } from '@angular/core'
import { SessionStorageService } from 'ngx-webstorage'
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http'
import { BehaviorSubject, EMPTY, Observable, Subject, catchError, map, of, takeUntil, tap } from 'rxjs'

import { IAccount } from '../model/account.model'
import { LanguageService } from 'src/app/shared/service/language.service'
import { Router } from '@angular/router'
// TODO: uncomment when memberservice is added or change the account service so that this logic is absent from the account service
//import { MSMemberService } from 'app/entities/member/member.service';

@Injectable({ providedIn: 'root' })
export class AccountService {
  // TODO: have unknown and offline instead of undefined and null
  private accountData = new BehaviorSubject<IAccount | null | undefined>(undefined)
  private isFetchingAccountData = false
  private stopFetchingAccountData = new Subject()
  private authenticated = false
  private logoutAsResourceUrl = '/services/userservice/api'

  constructor(
    // TODO: uncomment when language service is implemented
    //private languageService: JhiLanguageService,
    private languageService: LanguageService,
    private sessionStorage: SessionStorageService,
    private router: Router,
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
          this.isFetchingAccountData = false
          return EMPTY
        }),
        map((response: HttpResponse<IAccount>) => {
          this.isFetchingAccountData = false
          if (response && response.body) {
            this.authenticated = true
            const account: IAccount = response.body
            if (account.langKey) {
              this.languageService.updateLanguageCodeInUrl(account.langKey)
            }
            this.accountData.next(account)
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

  getMfaSetup(): Observable<{ secret: string; otp: string; qrCode: any }> {
    return this.http.get<any>('/services/userservice/api/account/mfa')
  }

  save(account: IAccount): Observable<boolean> {
    const headers = { 'Accept-Language': account.langKey }
    return this.http.post('/services/userservice/api/account', account, { observe: 'response', headers }).pipe(
      map((res: HttpResponse<any>) => this.isSuccess(res)),
      catchError((err) => {
        return of(false)
      })
    )
  }

  isSuccess(res: HttpResponse<any>): boolean {
    if (res.status == 200) {
      return true
    }
    return false
  }

  enableMfa(mfaSetup: any): Observable<string[] | null> {
    return this.http.post('/services/userservice/api/account/mfa/on', mfaSetup, { observe: 'response' }).pipe(
      map((res: HttpResponse<any>) => res.body),
      catchError((err) => {
        console.error('error enabling mfa')
        return of(null)
      })
    )
  }

  disableMfa(): Observable<boolean> {
    return this.http.post('/services/userservice/api/account/mfa/off', null, { observe: 'response' }).pipe(
      map((res: HttpResponse<any>) => this.isSuccess(res)),
      catchError((err) => {
        return of(false)
      })
    )
  }
  // TODO: any - this seems to only be used for logging out (only ever receives null as arg)
  clearAccountData() {
    this.accountData.next(null)
    this.authenticated = false
    this.router.navigate(['/login'])
  }

  hasAnyAuthority(authorities: string[]): boolean {
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
    if ((this.accountData.value === undefined && !this.isFetchingAccountData) || force) {
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

  getUsername(): string | null {
    let username: string | null = null

    if (this.isIdentityResolved()) {
      if (this.accountData.value?.firstName) {
        username = this.accountData.value!.firstName
      }
      if (this.accountData.value?.lastName) {
        if (username) {
          username = username + ' ' + this.accountData.value!.lastName
        } else {
          username = this.accountData.value!.lastName
        }
      }
      if (username == null) {
        username = this.accountData.value!.email
      }
    }
    return username
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
