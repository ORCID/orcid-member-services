import { HttpErrorResponse } from '@angular/common/http'
import { ErrorHandler, Injectable } from '@angular/core'
import { Observable, Subject } from 'rxjs'
import { AppError } from '../model/error.model'
import { Router } from '@angular/router'
import { LoginService } from 'src/app/account'

// To inject this service, you have to include '@Inject(ErrorHandler)' to be able to subscribe to observables, e.g.:
// @Inject(ErrorHandler) private errorService: ErrorService

@Injectable({ providedIn: 'root' })
export class ErrorService implements ErrorHandler {
  private errors: Subject<any> = new Subject<any>()
  NON_CHECKED_URLS = ['/', '/reset/request', '/reset/finish']

  constructor(
    private router: Router,
    private loginService: LoginService
  ) {}
  handleError(error: any) {
    console.log(error)
    if (error instanceof HttpErrorResponse || error.name === 'HttpErrorResponse') {
      if (error.status === 401) {
        if (this.loginService.isAuthenticated()) {
          this.loginService.logoutDirectly()
          this.router.navigate(['/'])
        } else if (!this.NON_CHECKED_URLS.find((x) => this.router.url.startsWith(x))) {
          this.loginService.logout()
          this.router.navigate(['/'])
        }
      } else {
        this.errors.next(new AppError(error.status, error.error.title || error.message))
      }
    } else {
      console.error('Unknown error occurred', error)
    }
  }

  on(): Observable<any> {
    return this.errors.asObservable()
  }
}
