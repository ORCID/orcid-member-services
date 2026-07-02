import { HttpErrorResponse } from '@angular/common/http'
import { ErrorHandler, Injectable } from '@angular/core'
import { Observable, Subject } from 'rxjs'
import { AppError } from '../model/error.model'

@Injectable({ providedIn: 'root' })
export class ErrorService implements ErrorHandler {
  private errors: Subject<any> = new Subject<any>()

  handleError(error: any) {
    if (error instanceof HttpErrorResponse) {
      let errorMessage = error.error?.title || error.message

      // Specifically handle 400 Bad Request
      if (error.status === 400 && error.error) {
        if (typeof error.error === 'string') {
          // If the server returned a plain text string body
          errorMessage = error.error
        } else if (typeof error.error === 'object') {
          // If the server returned a JSON object.
          // Look for common error fields (like 'detail' in RFC 7807),
          // or fallback to stringifying the whole object so it isn't lost.
          errorMessage = error.error.detail || error.error.message || JSON.stringify(error.error)
        }
      }

      this.errors.next(new AppError(error.status, errorMessage))
    } else {
      console.error('Unknown error occurred', error)
    }
  }

  on(): Observable<any> {
    return this.errors.asObservable()
  }
}
