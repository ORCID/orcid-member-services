import { Injectable } from '@angular/core';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LoginService {
  constructor(private accountService: AccountService, private authServerProvider: AuthServerProvider) {}

  login(credentials): Observable<any> {
    return this.authServerProvider.login(credentials);
  }

  isAuthenticated() {
    return this.accountService.isAuthenticated();
  }

  logoutDirectly() {
    this.accountService.authenticate(null);
  }

  logout() {
    if (this.accountService.isAuthenticated()) {
      this.authServerProvider.logout().subscribe(() => this.accountService.authenticate(null));
    } else {
      this.accountService.authenticate(null);
    }
  }
}
