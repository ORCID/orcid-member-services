import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { Observable } from 'rxjs'

@Injectable({ providedIn: 'root' })
export class PasswordResetInitService {
  constructor(private http: HttpClient) {}

  initPasswordReset(mail: string): Observable<any> {
    return this.http.post('/services/userservice/api/account/reset-password/init', mail)
  }
}
