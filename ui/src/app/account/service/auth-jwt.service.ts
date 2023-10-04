import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from '../../../app/app.constants';
import { ILoginCredentials } from '../model/login.model';

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  constructor(private http: HttpClient) {}

  getToken() {
    return null;
  }

  login(credentials: ILoginCredentials): Observable<any> {
    const data = {
      username: credentials.username,
      password: credentials.password,
      mfaCode: credentials.mfaCode
    };
    
    return this.http.post('/auth/login', data, {});
  }

  // TODO: not being used?
/*   loginWithToken(jwt, rememberMe) {
    if (jwt) {
      this.storeAuthenticationToken(jwt, rememberMe);
      return Promise.resolve(jwt);
    } else {
      return Promise.reject('auth-jwt-service Promise reject'); // Put appropriate error message here
    }
  } 

  storeAuthenticationToken(jwt, rememberMe) {}
*/

  logout(): Observable<any> {
    return this.http.post(SERVER_API_URL + 'auth/logout', null);
  }
}
