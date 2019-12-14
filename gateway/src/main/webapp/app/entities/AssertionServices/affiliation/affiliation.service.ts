import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IAffiliation } from 'app/shared/model/AssertionServices/affiliation.model';

type EntityResponseType = HttpResponse<IAffiliation>;
type EntityArrayResponseType = HttpResponse<IAffiliation[]>;

@Injectable({ providedIn: 'root' })
export class AffiliationService {
  public resourceUrl = SERVER_API_URL + 'services/assertionservices/assertion/api/affiliation';

  constructor(protected http: HttpClient) {}

  create(affiliation: IAffiliation): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(affiliation);
    return this.http
      .post<IAffiliation>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(affiliation: IAffiliation): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(affiliation);
    return this.http
      .put<IAffiliation>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http
      .get<IAffiliation>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IAffiliation[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(affiliation: IAffiliation): IAffiliation {
    const copy: IAffiliation = Object.assign({}, affiliation, {
      created: affiliation.created != null && affiliation.created.isValid() ? affiliation.created.toJSON() : null,
      modified: affiliation.modified != null && affiliation.modified.isValid() ? affiliation.modified.toJSON() : null,
      deletedFromORCID:
        affiliation.deletedFromORCID != null && affiliation.deletedFromORCID.isValid() ? affiliation.deletedFromORCID.toJSON() : null
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.created = res.body.created != null ? moment(res.body.created) : null;
      res.body.modified = res.body.modified != null ? moment(res.body.modified) : null;
      res.body.deletedFromORCID = res.body.deletedFromORCID != null ? moment(res.body.deletedFromORCID) : null;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((affiliation: IAffiliation) => {
        affiliation.created = affiliation.created != null ? moment(affiliation.created) : null;
        affiliation.modified = affiliation.modified != null ? moment(affiliation.modified) : null;
        affiliation.deletedFromORCID = affiliation.deletedFromORCID != null ? moment(affiliation.deletedFromORCID) : null;
      });
    }
    return res;
  }
}
