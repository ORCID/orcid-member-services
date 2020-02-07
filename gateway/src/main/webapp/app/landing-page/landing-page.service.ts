import { Injectable } from '@angular/core';
import { HttpClient, HttpClientModule, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LandingPageService {
  private headers: HttpHeaders;
  //TODO: Define sandbox and prod submit uri, current endpoint is: https://assertion-service.orcid.org/api/id-token
  const assertionServiceSubmitUri:string = 'https://en2t82yaxhwvz.x.pipedream.net/sample/post/request/';  
      
  constructor(private http: HttpClient) {
    this.headers = new HttpHeaders(
    {
      'Access-Control-Allow-Origin':'*',
      'Content-Type': 'application/json'
    });
  }

  submitUserResponse(data) {
    return this.http.post( 
      this.assertionServiceSubmitUri, 
      JSON.stringify(data), 
      { headers: this.headers }
    )   
  }
}
