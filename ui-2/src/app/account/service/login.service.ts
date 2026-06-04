import { Injectable, Injector, inject } from '@angular/core'
import { AccountService } from './account.service'
import { AuthServerProvider } from './auth-jwt.service'
import { map, Observable } from 'rxjs'
import { ILoginCredentials, ILoginResult } from '../model/login.model'
import { AuthenticatedResult, OidcSecurityService } from 'angular-auth-oidc-client'

@Injectable({ providedIn: 'root' })
export class LoginService {
  private injector = inject(Injector)
  private authServerProvider = inject(AuthServerProvider)
  private oidcSecurityService = inject(OidcSecurityService)

  login(credentials: ILoginCredentials): Observable<any> {
    return this.authServerProvider.login(credentials)
  }

  isAuthenticated(): Observable<boolean> {
    return this.oidcSecurityService.isAuthenticated$.pipe(map((result: AuthenticatedResult) => result.isAuthenticated))
  }

  logout() {
    // to break cyclic dependency
    const accountService = this.injector.get(AccountService)
    accountService.clearAccountData()
    this.authServerProvider.logout()
  }
}
