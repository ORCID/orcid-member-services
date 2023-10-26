import { Injectable } from '@angular/core'
import { AccountService } from './account.service'
import { AuthServerProvider } from './auth-jwt.service'
import { Observable } from 'rxjs'
import { ILoginCredentials, ILoginResult } from '../model/login.model'

@Injectable({ providedIn: 'root' })
export class LoginService {
  constructor(
    private accountService: AccountService,
    private authServerProvider: AuthServerProvider
  ) {}

  login(credentials: ILoginCredentials): Observable<ILoginResult> {
    return this.authServerProvider.login(credentials)
  }

  isAuthenticated() {
    return this.accountService.isAuthenticated()
  }

  logoutDirectly() {
    this.accountService.authenticate(null)
  }

  logout() {
    if (this.accountService.isAuthenticated()) {
      this.authServerProvider.logout().subscribe(() => this.accountService.authenticate(null))
    } else {
      this.accountService.authenticate(null)
    }
  }
}
