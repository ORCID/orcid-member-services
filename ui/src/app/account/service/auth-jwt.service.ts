import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs'

import { ILoginCredentials, ILoginResult } from '../model/login.model'
import { environment } from 'src/environments/environment'

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  constructor(private http: HttpClient) {}

  getToken() {
    return null
  }

  login(credentials: ILoginCredentials): Observable<ILoginResult> {
    return this.http.post<ILoginResult>('/auth/login', credentials)
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
    return this.http.post(environment.SERVER_API_URL + '/auth/logout', null)
  }
}
