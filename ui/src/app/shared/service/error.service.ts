import { ErrorHandler, Injectable } from '@angular/core'
import { Observable, Subject } from 'rxjs'

// To inject this service, you have to include '@Inject(ErrorHandler)' to be able to subscribe to observables, e.g.:
// @Inject(ErrorHandler) private errorService: ErrorService

@Injectable({ providedIn: 'root' })
export class ErrorService implements ErrorHandler {
  private errors: Subject<any> = new Subject<any>()

  handleError(error: any): void {
    console.error('Unknown error:', error)
    this.errors.next(error)
  }

  on(): Observable<any> {
    return this.errors.asObservable()
  }
}
