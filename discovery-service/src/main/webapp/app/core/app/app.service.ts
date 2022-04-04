import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from 'app/app.constants';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AppService {
  constructor(private http: HttpClient) {}

  getVersion(): Observable<any> {
    return this.http.get<any>(SERVER_API_URL + 'api/version', { observe: 'response' });
  }
}
