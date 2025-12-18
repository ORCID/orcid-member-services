import { Injectable } from '@angular/core'
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http'
import { Observable } from 'rxjs'
import { tap } from 'rxjs/operators'
import { Router } from '@angular/router'
import { LoginService } from 'src/app/account'

@Injectable()
export class AuthExpiredInterceptor implements HttpInterceptor {
  NON_CHECKED_URLS = ['/', '/reset/request', '/reset/finish']

  constructor(
    private router: Router,
    private loginService: LoginService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap({
        error: (err: any) => {
          if (err instanceof HttpErrorResponse) {
            if (err.status === 401) {
              if (this.loginService.isAuthenticated()) {
                this.loginService.logout()
                this.router.navigate(['/'])
              } else if (!this.NON_CHECKED_URLS.find((x) => this.router.url.startsWith(x))) {
                this.loginService.logout()
                this.router.navigate(['/'])
              }
            }
          }
        },
      })
    )
  }
}
