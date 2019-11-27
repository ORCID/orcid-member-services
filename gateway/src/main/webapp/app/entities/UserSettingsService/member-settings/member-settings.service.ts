import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';

type EntityResponseType = HttpResponse<IMemberSettings>;
type EntityArrayResponseType = HttpResponse<IMemberSettings[]>;

@Injectable({ providedIn: 'root' })
export class MemberSettingsService {
  public resourceUrl = SERVER_API_URL + 'services/usersettingsservice/api/member-settings';

  constructor(protected http: HttpClient) {}

  create(memberSettings: IMemberSettings): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(memberSettings);
    return this.http
      .post<IMemberSettings>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(memberSettings: IMemberSettings): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(memberSettings);
    return this.http
      .put<IMemberSettings>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http
      .get<IMemberSettings>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IMemberSettings[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(memberSettings: IMemberSettings): IMemberSettings {
    const copy: IMemberSettings = Object.assign({}, memberSettings, {
      createdDate: memberSettings.createdDate != null && memberSettings.createdDate.isValid() ? memberSettings.createdDate.toJSON() : null,
      lastModifiedDate:
        memberSettings.lastModifiedDate != null && memberSettings.lastModifiedDate.isValid()
          ? memberSettings.lastModifiedDate.toJSON()
          : null
    });
    return copy;
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdDate = res.body.createdDate != null ? moment(res.body.createdDate) : null;
      res.body.lastModifiedDate = res.body.lastModifiedDate != null ? moment(res.body.lastModifiedDate) : null;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((memberSettings: IMemberSettings) => {
        memberSettings.createdDate = memberSettings.createdDate != null ? moment(memberSettings.createdDate) : null;
        memberSettings.lastModifiedDate = memberSettings.lastModifiedDate != null ? moment(memberSettings.lastModifiedDate) : null;
      });
    }
    return res;
  }
}
