import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';

@Injectable({ providedIn: 'root' })
export class PasswordResetFinishService {
  constructor(private http: HttpClient) {}

  save(keyAndPassword: any): Observable<any> {
    return this.http.post(SERVER_API_URL + 'services/userservice/api/account/reset-password/finish', keyAndPassword);
  }

  validateKey(key: any): Observable<HttpResponse<any>> {
    return this.http.post(SERVER_API_URL + 'services/userservice/api/account/reset-password/validate', key, { observe: 'response' });
  }

  resendActivationEmail(key: any): Observable<HttpResponse<any>> {
    return this.http.post(SERVER_API_URL + 'services/userservice/api/users/' + key.key + '/resendActivation', {}, { observe: 'response' });
  }
}
