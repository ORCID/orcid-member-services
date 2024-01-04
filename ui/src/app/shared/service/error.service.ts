import { HttpErrorResponse } from '@angular/common/http'
import { ErrorHandler } from '@angular/core'
import { Observable } from 'rxjs/internal/Observable'
import { Subject } from 'rxjs/internal/Subject'
import { Error } from '../model/error.model'

export class ErrorService implements ErrorHandler {
  private errors = new Subject<Error>()

  on(): Observable<Error> {
    return this.errors
  }

  handleError(error: any) {
    if (error instanceof HttpErrorResponse) {
      this.errors.next(new Error(error.status, error.message))
    } else {
      console.error('Unknown error occurred', error)
    }
  }
}
