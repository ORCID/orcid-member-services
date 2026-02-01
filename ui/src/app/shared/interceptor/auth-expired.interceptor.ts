import { Injectable } from '@angular/core'
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http'
import { Observable } from 'rxjs'
import { tap } from 'rxjs/operators'
import { Router } from '@angular/router'
import { LoginService } from 'src/app/account'
import { OidcSecurityService } from 'angular-auth-oidc-client'

@Injectable()
export class AuthExpiredInterceptor implements HttpInterceptor {
  NON_CHECKED_URLS = ['/', '/reset/request', '/reset/finish']

  constructor(
    private router: Router,
    private loginService: LoginService,
    private oidcSecurityService: OidcSecurityService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap({
        error: (err: any) => {
          if (err instanceof HttpErrorResponse) {
            if (err.status === 401) {
              const token = this.oidcSecurityService.getAccessToken()
              if (token) {
                console.warn('Caught 401 with token present. Triggering logout.')
                this.loginService.logout()
              } else if (!this.NON_CHECKED_URLS.find((x) => this.router.url.startsWith(x))) {
                console.warn('Caught 401 but no token present. Ignoring logout trigger.')
              }
            }
          }
        },
      })
    )
  }
}
