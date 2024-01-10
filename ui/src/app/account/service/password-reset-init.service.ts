import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable, map, catchError, of } from 'rxjs'
import { PasswordResetInitResult } from '../model/password-reset-init-result.model'
import { EMAIL_NOT_FOUND_TYPE } from 'src/app/app.constants'

@Injectable({ providedIn: 'root' })
export class PasswordResetInitService {
  constructor(private http: HttpClient) {}

  initPasswordReset(mail: string): Observable<PasswordResetInitResult | null> {
    return this.http.post('/services/userservice/api/account/reset-password/init', mail, { observe: 'response' }).pipe(
      map((res: HttpResponse<any>) => this.getResult(res)),
      catchError((err) => {
        return of(null)
      })
    )
  }

  getResult(res: HttpResponse<any>): PasswordResetInitResult {
    if (res.status == 200) {
      return new PasswordResetInitResult(true, false, false)
    }

    if (res.status === 400 && res.body.error.type === EMAIL_NOT_FOUND_TYPE) {
      return new PasswordResetInitResult(false, false, true)
    }

    return new PasswordResetInitResult(false, true, false)
  }
}
