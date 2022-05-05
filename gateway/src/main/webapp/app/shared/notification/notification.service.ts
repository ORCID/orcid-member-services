import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssertionService } from '../../entities/AssertionService/assertion/assertion.service';

@Injectable()
export class NotificationService {
  resourceUrl: string;

  constructor(private http: HttpClient, private assertionService: AssertionService) {
    this.resourceUrl = this.assertionService.resourceUrl + '/notifications';
  }

  updateStatuses(): Observable<HttpResponse<any>> {
    return this.http.post<any>(this.resourceUrl, { observe: 'response' });
  }
}
