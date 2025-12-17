import { Injectable } from '@angular/core'
import { SessionStorageService } from 'ngx-webstorage'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { BehaviorSubject, EMPTY, Observable, Subject, catchError, map, of, takeUntil } from 'rxjs'

import { IAccount } from '../model/account.model'
import { LanguageService } from 'src/app/shared/service/language.service'
import { Router } from '@angular/router'
import { MemberService } from 'src/app/member/service/member.service'
import { OidcSecurityService } from 'angular-auth-oidc-client'

@Injectable({ providedIn: 'root' })
export class AccountService {
  // TODO: have custom 'unknown' and 'offline' statuses instead of 'undefined' and 'null'
  private accountData = new BehaviorSubject<IAccount | null | undefined>(undefined)
  private isFetchingAccountData = false
  private stopFetchingAccountData = new Subject()
  private authenticated = false

  constructor(
    private languageService: LanguageService,
    private sessionStorage: SessionStorageService,
    private router: Router,
    private http: HttpClient,
    private memberService: MemberService,
    private oidcSecurityService: OidcSecurityService
  ) {}

  private fetchAccountData(): Observable<IAccount | null> {
    this.isFetchingAccountData = true
    return this.http
      .get<IAccount>('/services/userservice/api/account', {
        observe: 'response',
      })
      .pipe(
        takeUntil(this.stopFetchingAccountData),
        map((response: HttpResponse<IAccount>) => {
          this.isFetchingAccountData = false
          if (response && response.body) {
            this.authenticated = true
            const account: IAccount = response.body
            if (account.langKey) {
              this.languageService.updateLanguageCodeInUrl(account.langKey)
            }
            this.accountData.next(account)
            return account // Return the account for the stream
          } else {
            this.handleError()
            return null
          }
        }),
        catchError(() => {
          this.handleError()
          return of(null)
        })
      )
  }

  private handleError() {
    this.authenticated = false
    this.accountData.next(null)
    this.memberService.setMemberData(undefined)
    this.isFetchingAccountData = false
  }

  getMfaSetup(): Observable<{ secret: string; otp: string; qrCode: any }> {
    return this.http.get<any>('/services/userservice/api/account/mfa')
  }

  save(account: IAccount): Observable<boolean> {
    const headers = { 'Accept-Language': account.langKey }
    return this.http.post('/services/userservice/api/account', account, { observe: 'response', headers }).pipe(
      map((res: HttpResponse<any>) => this.isSuccess(res)),
      catchError(() => {
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
      catchError(() => {
        console.error('error enabling mfa')
        return of(null)
      })
    )
  }

  disableMfa(userId: string): Observable<boolean> {
    return this.http.post(`/services/userservice/api/account/${userId}/mfa/off`, null, { observe: 'response' }).pipe(
      map((res: HttpResponse<any>) => this.isSuccess(res)),
      catchError(() => {
        return of(false)
      })
    )
  }
  clearAccountData() {
    this.accountData.next(null)
    this.authenticated = false
    this.router.navigate(['/login'])

    this.oidcSecurityService.logoff().subscribe(() => {
      this.router.navigate(['/login'])
    })
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
      this.memberService.stopFetchingMemberData.next(true)
      this.memberService.setMemberData(undefined)
      this.stopFetchingAccountData.next(true)
    }
    if ((this.accountData.value === undefined && !this.isFetchingAccountData) || force) {
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
}
