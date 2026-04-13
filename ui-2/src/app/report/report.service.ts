import { Injectable, inject } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs'

@Injectable({ providedIn: 'root' })
export class ReportService {
  protected http = inject(HttpClient)

  public resourceUrl = '/memberservice/api/reports'

  getDashboardInfo(reportType: string): Observable<{ url: any; jwt: any }> {
    return this.http.get<{ url: any; jwt: any }>(`${this.resourceUrl}/` + reportType)
  }
}
