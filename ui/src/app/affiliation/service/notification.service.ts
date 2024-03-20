import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs'
import * as moment from 'moment'
import { map } from 'rxjs/operators'
import { AffiliationPage, IAffiliation, IAffiliationPage } from '../model/affiliation.model'
import { createRequestOption } from 'src/app/shared/request-util'
import { AffiliationService } from './affiliation.service'

@Injectable({ providedIn: 'root' })
export class NotificationService {
  resourceUrl: string

  constructor(
    private http: HttpClient,
    private assertionService: AffiliationService
  ) {
    this.resourceUrl = this.assertionService.resourceUrl + '/notification-request'
  }

  updateStatuses(language: string): Observable<any> {
    return this.http.post<any>(this.resourceUrl, { language })
  }

  requestInProgress(): Observable<any> {
    return this.http.get<any>(this.resourceUrl)
  }
}
