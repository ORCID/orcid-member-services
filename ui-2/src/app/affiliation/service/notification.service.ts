import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { Observable } from 'rxjs'
import { AffiliationService } from './affiliation.service'

@Injectable({ providedIn: 'root' })
export class NotificationService {
  resourceUrl: string

  constructor(
    private http: HttpClient,
    private affiliationService: AffiliationService
  ) {
    this.resourceUrl = this.affiliationService.resourceUrl + '/notification-request'
  }

  updateStatuses(language: string): Observable<any> {
    return this.http.post<any>(this.resourceUrl, { language })
  }

  requestInProgress(): Observable<any> {
    return this.http.get<any>(this.resourceUrl)
  }
}
