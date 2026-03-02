import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs'

@Injectable({ providedIn: 'root' })
export class ReportService {
  public resourceUrl = '/services/memberservice/api/reports'

  constructor(protected http: HttpClient) {}

  getDashboardInfo(reportType: string): Observable<{ url: any; jwt: any }> {
    return this.http.get<{ url: any; jwt: any }>(`${this.resourceUrl}/` + reportType)
  }
}
