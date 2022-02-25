import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IAssertion } from 'app/shared/model/AssertionService/assertion.model';
import { ErrorHandlerService } from 'app/shared/errorHandler/error-handler.service';

type EntityResponseType = HttpResponse<IAssertion>;
type EntityArrayResponseType = HttpResponse<IAssertion[]>;

@Injectable({ providedIn: 'root' })
export class AssertionService {
  public resourceUrl = SERVER_API_URL + 'services/assertionservice/api/assertion';

  constructor(protected http: HttpClient, protected errorHandler: ErrorHandlerService) {}

  create(assertion: IAssertion): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(assertion);
    return this.http
      .post<IAssertion>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  upload(assertion: IAssertion): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(assertion);
    return this.http
      .post<IAssertion>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(assertion: IAssertion): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(assertion);
    return this.http
      .put<IAssertion>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http
      .get<IAssertion>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IAssertion[]>(this.resourceUrl + 's', { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  deleteFromOrcid(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/orcid/${id}`, { observe: 'response' });
  }

  generatePermissionLinks(): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/permission-links`, { observe: 'response' });
  }

  generateCSV(): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/csv`, { observe: 'response' });
  }

  generateReport(): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/report`, { observe: 'response' });
  }

  protected convertDateFromClient(assertion: IAssertion): IAssertion {
    const copy: IAssertion = Object.assign({}, assertion, {
      created: assertion.created != null && assertion.created.isValid() ? assertion.created.toJSON() : null,
      modified: assertion.modified != null && assertion.modified.isValid() ? assertion.modified.toJSON() : null,
      deletedFromORCID:
        assertion.deletedFromORCID != null && assertion.deletedFromORCID.isValid() ? assertion.deletedFromORCID.toJSON() : null
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.created = res.body.created != null ? moment(res.body.created) : null;
      res.body.modified = res.body.modified != null ? moment(res.body.modified) : null;
      res.body.addedToORCID = res.body.addedToORCID != null ? moment(res.body.addedToORCID) : null;
      res.body.deletedFromORCID = res.body.deletedFromORCID != null ? moment(res.body.deletedFromORCID) : null;
      res.body.updatedInORCID = res.body.updatedInORCID != null ? moment(res.body.updatedInORCID) : null;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((assertion: IAssertion) => {
        assertion.created = assertion.created != null ? moment(assertion.created) : null;
        assertion.modified = assertion.modified != null ? moment(assertion.modified) : null;
        assertion.deletedFromORCID = assertion.deletedFromORCID != null ? moment(assertion.deletedFromORCID) : null;
        assertion.addedToORCID = assertion.addedToORCID != null ? moment(assertion.addedToORCID) : null;
        assertion.updatedInORCID = assertion.updatedInORCID != null ? moment(assertion.updatedInORCID) : null;
      });
    }
    return res;
  }
}
