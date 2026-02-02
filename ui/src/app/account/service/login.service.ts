import { Injectable } from '@angular/core'
import { AccountService } from './account.service'
import { AuthServerProvider } from './auth-jwt.service'
import { map, Observable } from 'rxjs'
import { ILoginCredentials, ILoginResult } from '../model/login.model'
import { AuthenticatedResult, OidcSecurityService } from 'angular-auth-oidc-client'

@Injectable({ providedIn: 'root' })
export class LoginService {
  constructor(
    private accountService: AccountService,
    private authServerProvider: AuthServerProvider,
    private oidcSecurityService: OidcSecurityService
  ) {}

  login(credentials: ILoginCredentials): Observable<any> {
    return this.authServerProvider.login(credentials)
  }

  isAuthenticated(): Observable<boolean> {
    return this.oidcSecurityService.isAuthenticated$.pipe(map((result: AuthenticatedResult) => result.isAuthenticated))
  }

  logout() {
    this.accountService.clearAccountData()
    this.authServerProvider.logout()
  }
}
