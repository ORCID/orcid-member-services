import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { Observable } from 'rxjs'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { ILoginCredentials } from '../model/login.model'

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  constructor(
    private http: HttpClient,
    private oidcSecurityService: OidcSecurityService
  ) {}

  // 1. Return the token from the library instead of null
  getToken(): Observable<string> {
    return this.oidcSecurityService.getAccessToken()
  }

  // 2. Point to the new backend endpoint on :9000
  // Note: Spring expects 'mfa_code' based on our MfaDetailsSource
  login(credentials: ILoginCredentials): Observable<any> {
    return this.http.post<any>('/userservice/login', null, {
      params: {
        username: credentials.username,
        password: credentials.password,
        mfa_code: credentials.mfaCode ?? '',
      },
    })
  }

  logout(): Observable<any> {
    return this.oidcSecurityService.logoff()
  }
}
