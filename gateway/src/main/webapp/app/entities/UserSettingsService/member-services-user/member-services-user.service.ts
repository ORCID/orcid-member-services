import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';

type EntityResponseType = HttpResponse<IMemberServicesUser>;
type EntityArrayResponseType = HttpResponse<IMemberServicesUser[]>;

@Injectable({ providedIn: 'root' })
export class MemberServicesUserService {
  public resourceUrl = SERVER_API_URL + 'services/usersettingsservice/api/member-services-users';

  constructor(protected http: HttpClient) {}

  create(memberServicesUser: IMemberServicesUser): Observable<EntityResponseType> {
    return this.http.post<IMemberServicesUser>(this.resourceUrl, memberServicesUser, { observe: 'response' });
  }

  update(memberServicesUser: IMemberServicesUser): Observable<EntityResponseType> {
    return this.http.put<IMemberServicesUser>(this.resourceUrl, memberServicesUser, { observe: 'response' });
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IMemberServicesUser>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IMemberServicesUser[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
