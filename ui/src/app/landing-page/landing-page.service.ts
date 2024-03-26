import { Injectable } from '@angular/core'
import { HttpClient, HttpClientModule, HttpHeaders } from '@angular/common/http'
import { Observable } from 'rxjs'
import { ORCID_BASE_URL } from '../app.constants'

@Injectable({ providedIn: 'root' })
export class LandingPageService {
  private headers: HttpHeaders

  idTokenUri: string = '/services/assertionservice/api/id-token'
  recordConnectionUri: string = '/services/assertionservice/api/assertion/record/'
  memberInfoUri: string = '/services/memberservice/api/members/authorized/'
  userInfoUri: string = ORCID_BASE_URL + '/oauth/userinfo'
  publicKeyUri: string = ORCID_BASE_URL + '/oauth/jwks'

  constructor(private http: HttpClient) {
    this.headers = new HttpHeaders({
      'Access-Control-Allow-Origin': '*',
      'Content-Type': 'application/json',
    })
  }

  submitUserResponse(data: any): Observable<any> {
    return this.http.post(this.idTokenUri, JSON.stringify(data), { headers: this.headers })
  }

  getOrcidConnectionRecord(state: String): Observable<any> {
    const requestUrl = this.recordConnectionUri + state
    return this.http.get(requestUrl, { observe: 'response' })
  }

  getMemberInfo(state: String): Observable<any> {
    const requestUrl = this.memberInfoUri + state
    return this.http.get(requestUrl)
  }

  getUserInfo(access_token: String): Observable<any> {
    const headers = new HttpHeaders({
      Authorization: 'Bearer ' + access_token,
      'Content-Type': 'application/json',
    })
    return this.http.post(this.userInfoUri, {}, { headers })
  }

  getPublicKey(): Observable<any> {
    return this.http.get(this.publicKeyUri)
  }
}
