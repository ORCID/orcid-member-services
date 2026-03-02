import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs'
import { ILoginCredentials, ILoginResult } from '../model/login.model'

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  constructor(private http: HttpClient) {}

  getToken() {
    return null
  }

  login(credentials: ILoginCredentials): Observable<ILoginResult> {
    return this.http.post<ILoginResult>('/auth/login', credentials)
  }

  logout(): Observable<any> {
    return this.http.post('/auth/logout', null)
  }
}
