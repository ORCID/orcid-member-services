import { HttpErrorResponse } from '@angular/common/http'
import { ErrorHandler, Injectable } from '@angular/core'
import { Observable, Subject } from 'rxjs'
import { AppError } from '../model/error.model'

// To inject this service, you have to include '@Inject(ErrorHandler)' to be able to subscribe to observables, e.g.:
// @Inject(ErrorHandler) private errorService: ErrorService

@Injectable({ providedIn: 'root' })
export class ErrorService implements ErrorHandler {
  private errors: Subject<any> = new Subject<any>()

  handleError(error: any) {
    if (error instanceof HttpErrorResponse) {
      if (error.headers.has('errmmmmm')) {
        const i18nKey: string | null = error.headers.get('errmmmmm')
        this.errors.next(new AppError(error.status, error.message, i18nKey))
      } else {
        this.errors.next(new AppError(error.status, error.message, null))
      }
    } else {
      console.error('Unknown error occurred', error)
    }
  }

  on(): Observable<any> {
    return this.errors.asObservable()
  }
}
