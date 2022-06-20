import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable, Subject, ReplaySubject } from 'rxjs';
import { share, shareReplay } from 'rxjs/operators';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMSMember } from 'app/shared/model/member.model';

type EntityResponseType = HttpResponse<IMSMember>;
type EntityArrayResponseType = HttpResponse<IMSMember[]>;

@Injectable({ providedIn: 'root' })
export class MSMemberService {
  public resourceUrl = SERVER_API_URL + 'services/memberservice/api/members';
  public resourceUrl2 = SERVER_API_URL + 'services/memberservice/api/member-details';
  public allMembers$: Observable<EntityArrayResponseType>;
  public orgNameMap: any;

  constructor(protected http: HttpClient) {
    console.log(SERVER_API_URL);
    this.allMembers$ = this.getAllMembers().pipe(share());
    this.orgNameMap = new Object();
  }

  create(msMember: IMSMember): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(msMember);
    return this.http
      .post<IMSMember>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(msMember: IMSMember): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(msMember);
    return this.http
      .put<IMSMember>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  validate(msMember: IMSMember): Observable<any> {
    const copy = this.convertDateFromClient(msMember);
    return this.http
      .post<IMSMember>(this.resourceUrl + '/validate', copy, { observe: 'response' })
      .pipe(map((res: any) => this.convertDateFromServer(res)));
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http
      .get<IMSMember>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IMSMember[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  getAllMembers(): Observable<EntityArrayResponseType> {
    return this.http
      .get<IMSMember[]>(`${this.resourceUrl}/list/all`, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  getMember(): Observable<EntityResponseType> {
    return this.http
      .get(`${this.resourceUrl2}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  protected convertDateFromClient(msMember: IMSMember): IMSMember {
    const copy: IMSMember = Object.assign({}, msMember, {
      createdDate: msMember.createdDate != null && msMember.createdDate.isValid() ? msMember.createdDate.toJSON() : null,
      lastModifiedDate: msMember.lastModifiedDate != null && msMember.lastModifiedDate.isValid() ? msMember.lastModifiedDate.toJSON() : null
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
      res.body.forEach((msMember: IMSMember) => {
        msMember.createdDate = msMember.createdDate != null ? moment(msMember.createdDate) : null;
        msMember.lastModifiedDate = msMember.lastModifiedDate != null ? moment(msMember.lastModifiedDate) : null;
      });
    }
    return res;
  }
}
