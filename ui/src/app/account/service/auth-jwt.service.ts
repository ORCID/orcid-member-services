import { Injectable } from '@angular/core'
import { HttpClient, HttpHeaders } from '@angular/common/http'
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

  login(credentials: ILoginCredentials): Observable<any> {
    const body = new URLSearchParams()
    body.set('username', credentials.username)
    body.set('password', credentials.password)

    if (credentials.mfaCode) {
      body.set('mfa_code', credentials.mfaCode)
    }

    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
    })

    return this.http.post('/userservice/account/login', body.toString(), {
      headers: headers,
      responseType: 'json',
    })
  }

  logout(): Observable<any> {
    return this.oidcSecurityService.logoff()
  }
}
