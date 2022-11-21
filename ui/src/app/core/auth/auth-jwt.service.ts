import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  constructor(private http: HttpClient) {}

  getToken() {
    return null;
  }
  // TODO
  login(credentials:any): Observable<any> {
    const data = {
      username: credentials.username,
      password: credentials.password,
      rememberMe: credentials.crememberMe,
      mfaCode: credentials.mfaCode
    };
    return this.http.post(SERVER_API_URL + 'auth/login', data, {});
  }
  // TODO
  loginWithToken(jwt:any, rememberMe:any) {
    if (jwt) {
      this.storeAuthenticationToken(jwt, rememberMe);
      return Promise.resolve(jwt);
    } else {
      return Promise.reject('auth-jwt-service Promise reject'); // Put appropriate error message here
    }
  }
  // TODO
  storeAuthenticationToken(jwt:any, rememberMe:any) {}

  logout(): Observable<any> {
    return this.http.post(SERVER_API_URL + 'auth/logout', null);
  }
}
