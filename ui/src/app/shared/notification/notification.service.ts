import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssertionService } from '../../entities/assertion/assertion.service';

@Injectable()
export class NotificationService {
  resourceUrl: string;

  constructor(private http: HttpClient, private assertionService: AssertionService) {
    this.resourceUrl = this.assertionService.resourceUrl + '/notification-request';
  }

  updateStatuses(): Observable<HttpResponse<any>> {
    return this.http.post<any>(this.resourceUrl, { observe: 'response' });
  }

  requestInProgress(): Observable<HttpResponse<any>> {
    return this.http.get<any>(this.resourceUrl, { observe: 'response' });
  }
}
