import { Injectable, inject } from '@angular/core'
import { HttpClient, HttpHeaders } from '@angular/common/http'
import { Observable } from 'rxjs'
import { ORCID_BASE_URL } from '../app.constants'
import { OrcidRecord } from '../shared/model/orcid-record.model'

@Injectable({ providedIn: 'root' })
export class LandingPageService {
  private http = inject(HttpClient)

  private headers: HttpHeaders

  idTokenUri = '/assertionservice/assertions/id-token'
  recordConnectionUri = '/assertionservice/assertions/record/'
  salesforceIdUrl = '/assertionservice/assertions/salesforce/'
  memberInfoUri = '/memberservice/members/authorized/'
  userInfoUri = ORCID_BASE_URL + '/oauth/userinfo'
  publicKeyUri = ORCID_BASE_URL + '/oauth/jwks'

  constructor() {
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

  getSalesforceId(state: string): Observable<string> {
    const requestUrl = this.salesforceIdUrl + state
    return this.http.get(requestUrl, { responseType: 'text' })
  }

  getMemberInfo(salesforceId: string): Observable<any> {
    const requestUrl = this.memberInfoUri + salesforceId
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
