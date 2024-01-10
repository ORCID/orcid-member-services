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
    this.accountService.clearAccountData()
  }

  logout() {
    if (this.accountService.isAuthenticated()) {
      this.authServerProvider.logout().subscribe(() => this.accountService.clearAccountData())
    } else {
      this.accountService.clearAccountData()
    }
  }
}
