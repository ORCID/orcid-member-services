import { HttpErrorResponse } from '@angular/common/http'
import { ErrorHandler } from '@angular/core'
import { Observable } from 'rxjs/internal/Observable'
import { Subject } from 'rxjs/internal/Subject'
import { AppError } from '../model/error.model'

export class ErrorService implements ErrorHandler {
  private errors = new Subject<AppError>()

  on(): Observable<AppError> {
    return this.errors
  }

  handleError(error: any) {
    if (error instanceof HttpErrorResponse) {
      this.errors.next(new AppError(error.status, error.message))
    } else {
      console.error('Unknown error occurred', error)
    }
  }
}
