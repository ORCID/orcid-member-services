import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';

@Injectable({ providedIn: 'root' })
export class ReportService {
  public resourceUrl = SERVER_API_URL + 'services/memberservice/api/reports';

  constructor(protected http: HttpClient) {}

  getDashboardInfo(reportType: string): Observable<HttpResponse<any>> {
    return this.http.get<String>(`${this.resourceUrl}/` + reportType, { observe: 'response' });
  }
}
