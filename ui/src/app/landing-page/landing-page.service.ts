import { Injectable } from '@angular/core'
import { HttpClient, HttpHeaders } from '@angular/common/http'
import { Observable } from 'rxjs'
import { ORCID_BASE_URL } from '../app.constants'
import { OrcidRecord } from '../shared/model/orcid-record.model'

@Injectable({ providedIn: 'root' })
export class LandingPageService {
  private headers: HttpHeaders

  idTokenUri = '/services/assertionservice/api/id-token'
  recordConnectionUri = '/services/assertionservice/api/assertion/record/'
  memberInfoUri = '/services/memberservice/api/members/authorized/'
  userInfoUri = ORCID_BASE_URL + '/oauth/userinfo'
  publicKeyUri = ORCID_BASE_URL + '/oauth/jwks'

  constructor(private http: HttpClient) {
    this.headers = new HttpHeaders({
      'Access-Control-Allow-Origin': '*',
      'Content-Type': 'application/json',
    })
  }

  submitUserResponse(data: any): Observable<any> {
    return this.http.post(this.idTokenUri, JSON.stringify(data), { headers: this.headers })
  }

  getOrcidConnectionRecord(state: string): Observable<OrcidRecord> {
    const requestUrl = this.recordConnectionUri + state
    return this.http.get<OrcidRecord>(requestUrl)
  }

  getMemberInfo(state: string): Observable<any> {
    const requestUrl = this.memberInfoUri + state
    return this.http.get(requestUrl)
  }

  getUserInfo(access_token: string): Observable<any> {
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
