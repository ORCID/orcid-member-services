import { Injectable } from '@angular/core';
import { HttpClient, HttpClientModule, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';

@Injectable({ providedIn: 'root' })
export class LandingPageService {
  private headers: HttpHeaders;
  // TODO: Define sandbox and prod submit uri, current endpoint is: https://assertionservice.orcid.org/api/id-token
  // const assertionServiceSubmitUri:string = 'https://en2t82yaxhwvz.x.pipedream.net/sample/post/request/';
  idTokenUri: string = SERVER_API_URL + 'services/assertionservice/api/id-token';
  recordConnectionUri: string = SERVER_API_URL + 'services/assertionservice/api/assertion/record/';
  memberInfoUri: string = SERVER_API_URL + 'services/memberservice/api/members/authorized/';

  constructor(private http: HttpClient) {
    this.headers = new HttpHeaders({
      'Access-Control-Allow-Origin': '*',
      'Content-Type': 'application/json'
    });
  }

  submitUserResponse(data) {
    return this.http.post(this.idTokenUri, JSON.stringify(data), { headers: this.headers });
  }

  getOrcidConnectionRecord(state: String): Observable<any> {
    let requestUrl = this.recordConnectionUri + state;
    return this.http
      .get(requestUrl, { observe: 'response' });
  }

  getMemberInfo(state: String): Observable<any> {
    let requestUrl = this.memberInfoUri + state;
    return this.http
      .get(requestUrl, { observe: 'response' });
  }
}
