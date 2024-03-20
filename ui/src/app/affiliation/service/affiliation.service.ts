import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { Observable } from 'rxjs'
import * as moment from 'moment'
import { map } from 'rxjs/operators'
import { AffiliationPage, IAffiliation, IAffiliationPage } from '../model/affiliation.model'
import { createRequestOption } from 'src/app/shared/request-util'

@Injectable({ providedIn: 'root' })
export class AffiliationService {
  public resourceUrl = '/services/assertionservice/api/assertion'

  constructor(protected http: HttpClient) {}

  create(affiliation: IAffiliation): Observable<IAffiliation> {
    const copy = this.convertDateFromClient(affiliation)
    return this.http
      .post<IAffiliation>(this.resourceUrl, copy)
      .pipe(map((res: IAffiliation) => this.convertDateFromServer(res)))
  }

  upload(affiliation: IAffiliation): Observable<IAffiliation> {
    const copy = this.convertDateFromClient(affiliation)
    return this.http
      .post<IAffiliation>(this.resourceUrl, copy)
      .pipe(map((res: IAffiliation) => this.convertDateFromServer(res)))
  }

  update(affiliation: IAffiliation): Observable<IAffiliation> {
    const copy = this.convertDateFromClient(affiliation)
    return this.http
      .put<IAffiliation>(this.resourceUrl, copy)
      .pipe(map((res: IAffiliation) => this.convertDateFromServer(res)))
  }

  find(id: string): Observable<IAffiliation> {
    return this.http
      .get<IAffiliation>(`${this.resourceUrl}/${id}`)
      .pipe(map((res: IAffiliation) => this.convertDateFromServer(res)))
  }

  query(req?: any): Observable<IAffiliationPage | null> {
    const options = createRequestOption(req)
    return this.http
      .get<IAffiliation[]>(this.resourceUrl + 's', { params: options, observe: 'response' })
      .pipe(map((res: HttpResponse<IAffiliation[]>) => this.convertToAffiliationPage(res)))
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' })
  }

  deleteFromOrcid(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/orcid/${id}`, { observe: 'response' })
  }

  generatePermissionLinks(): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/permission-links`, { observe: 'response' })
  }

  generateCSV(): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/csv`, { observe: 'response' })
  }

  generateReport(): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/report`, { observe: 'response' })
  }

  protected convertDateFromClient(affiliation: IAffiliation): IAffiliation {
    const copy: IAffiliation = Object.assign({}, affiliation, {
      created: affiliation.created != null && affiliation.created.isValid() ? affiliation.created.toJSON() : null,
      modified: affiliation.modified != null && affiliation.modified.isValid() ? affiliation.modified.toJSON() : null,
      deletedFromORCID:
        affiliation.deletedFromORCID != null && affiliation.deletedFromORCID.isValid()
          ? affiliation.deletedFromORCID.toJSON()
          : null,
    })
    return copy
  }

  protected convertDateFromServer(res: IAffiliation): IAffiliation {
    if (res) {
      res.created = res.created ? moment(res.created) : undefined
      res.modified = res.modified ? moment(res.modified) : undefined
      res.addedToORCID = res.addedToORCID ? moment(res.addedToORCID) : undefined
      res.deletedFromORCID = res.deletedFromORCID ? moment(res.deletedFromORCID) : undefined
      res.updatedInORCID = res.updatedInORCID ? moment(res.updatedInORCID) : undefined
    }
    return res
  }

  protected convertToAffiliationPage(res: HttpResponse<IAffiliation[]>): IAffiliationPage | null {
    if (res.body) {
      res.body.forEach((affiliation: IAffiliation) => {
        affiliation.created = affiliation.created ? moment(affiliation.created) : undefined
        affiliation.modified = affiliation.modified ? moment(affiliation.modified) : undefined
        affiliation.deletedFromORCID = affiliation.deletedFromORCID ? moment(affiliation.deletedFromORCID) : undefined
        affiliation.addedToORCID = affiliation.addedToORCID ? moment(affiliation.addedToORCID) : undefined
        affiliation.updatedInORCID = affiliation.updatedInORCID ? moment(affiliation.updatedInORCID) : undefined
      })
      const totalCount: string | null = res.headers.get('X-Total-Count')
      if (totalCount) {
        const userPage = new AffiliationPage(res.body, parseInt(totalCount, 10))
        return userPage
      }
    }
    return null
  }
}
