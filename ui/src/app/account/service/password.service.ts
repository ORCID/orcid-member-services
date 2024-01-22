import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable, map, catchError, of } from 'rxjs'
import { PasswordResetInitResult } from '../model/password-reset-init-result.model'
import { EMAIL_NOT_FOUND_TYPE } from 'src/app/app.constants'
import { KeyValidationResult } from '../model/key-validation-result'
import { ResendActivationEmailResult } from '../model/resend-activation-email-result'

@Injectable({ providedIn: 'root' })
export class PasswordService {
  constructor(private http: HttpClient) {}

  initPasswordReset(mail: string): Observable<PasswordResetInitResult | null> {
    return this.http.post('/services/userservice/api/account/reset-password/init', mail, { observe: 'response' }).pipe(
      map((res: HttpResponse<any>) => this.getPasswordResetResult(res)),
      catchError((err) => {
        return of(null)
      })
    )
  }

  updatePassword(newPassword: string, currentPassword: string): Observable<any> {
    return this.http.post('/services/userservice/api/account/change-password', {
      currentPassword,
      newPassword,
    })
  }

  savePassword(key: string, newPassword: string): Observable<any> {
    return this.http.post('/services/userservice/api/account/reset-password/finish', {key, newPassword:"a"})
  }

  validateKey(key: any): Observable<KeyValidationResult> {
    return this.http.post<KeyValidationResult>('/services/userservice/api/account/reset-password/validate', key)
  }

  resendActivationEmail(key: any): Observable<ResendActivationEmailResult> {
    return this.http.post<ResendActivationEmailResult>(
      '/services/userservice/api/users/' + key.key + '/resendActivation',
      {}
    )
  }

  getPasswordResetResult(res: HttpResponse<any>): PasswordResetInitResult {
    if (res.status == 200) {
      return new PasswordResetInitResult(true, false, false)
    }

    if (res.status === 400 && res.body.error.type === EMAIL_NOT_FOUND_TYPE) {
      return new PasswordResetInitResult(false, false, true)
    }

    return new PasswordResetInitResult(false, true, false)
  }
}
