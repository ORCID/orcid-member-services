import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, share } from 'rxjs/operators';
import * as moment from 'moment';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IMSMember } from 'app/shared/model/member.model';
import {
  ISFMemberData,
  ISFRawMemberData,
  SFMemberData,
  ISFRawConsortiumMemberData,
  SFConsortiumMemberData
} from 'app/shared/model/salesforce-member-data.model';

type EntityResponseType = HttpResponse<IMSMember>;
type EntityArrayResponseType = HttpResponse<IMSMember[]>;
type SalesforceEntityResponseType = HttpResponse<ISFRawMemberData>;

@Injectable({ providedIn: 'root' })
export class MSMemberService {
  public resourceUrl = SERVER_API_URL + 'services/memberservice/api';
  public allMembers$: Observable<EntityArrayResponseType>;
  public orgNameMap: any;
  public memberData: ISFMemberData;

  constructor(protected http: HttpClient) {
    this.allMembers$ = this.getAllMembers().pipe(share());
    this.orgNameMap = new Object();
  }

  create(msMember: IMSMember): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(msMember);
    return this.http
      .post<IMSMember>(`${this.resourceUrl}/members`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(msMember: IMSMember): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(msMember);
    return this.http
      .put<IMSMember>(`${this.resourceUrl}/members`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  validate(msMember: IMSMember): Observable<any> {
    const copy = this.convertDateFromClient(msMember);
    return this.http
      .post<IMSMember>(`${this.resourceUrl}/members/validate`, copy, { observe: 'response' })
      .pipe(map((res: any) => this.convertDateFromServer(res)));
  }

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IMSMember>(`${this.resourceUrl}/members/${id}`, { observe: 'response' }).pipe(
      map((res: EntityResponseType) => this.convertDateFromServer(res)),
      catchError(err => {
        return of(err);
      })
    );
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IMSMember[]>(`${this.resourceUrl}/members`, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  getAllMembers(): Observable<EntityArrayResponseType> {
    return this.http
      .get<IMSMember[]>(`${this.resourceUrl}/members/list/all`, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  getMember(): Observable<SFMemberData> {
    return this.http.get<ISFRawMemberData>(`${this.resourceUrl}/member-details`, { observe: 'response' }).pipe(
      map((res: SalesforceEntityResponseType) => this.convertToSalesforceMemberData(res)),
      catchError(err => {
        return of(err);
      })
    );
  }

  getMemberContacts(): Observable<any> {
    return this.http.get<ISFRawMemberData>(`${this.resourceUrl}/member-contacts`, { observe: 'response' }).pipe(
      map((res: SalesforceEntityResponseType) => this.convertToSalesforceMemberData(res)),
      catchError(err => {
        return of(err);
      })
    );
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/members/${id}`, { observe: 'response' });
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

  protected convertToSalesforceMemberData(res: SalesforceEntityResponseType): SFMemberData {
    if (res.body) {
      return {
        ...new SFMemberData(),
        id: res.body.Id,
        consortiaMember: res.body.Consortia_Member__c,
        consortiaLeadId: res.body.Consortium_Lead__c,
        name: res.body.Name,
        publicDisplayName: res.body.Public_Display_Name__c,
        website: res.body.Website,
        billingCountry: res.body.BillingCountry,
        memberType: res.body.Research_Community__c,
        publicDisplayDescriptionHtml: res.body.Public_Display_Description__c,
        logoUrl: res.body.Logo_Description__c,
        publicDisplayEmail: res.body.Public_Display_Email__c,
        membershipStartDateString: res.body.Last_membership_start_date__c,
        membershipEndDateString: res.body.Last_membership_end_date__c,
        consortiumMembers: res.body.consortiumOpportunities ? this.convertToConsortiumMembers(res.body.consortiumOpportunities) : null
      };
    } else {
      return new SFMemberData();
    }
  }

  protected convertToConsortiumMembers(consortiumOpportunities: ISFRawConsortiumMemberData[]): SFConsortiumMemberData[] {
    let consortiumMembers: SFConsortiumMemberData[] = [];
    for (const consortiumOpportunity of consortiumOpportunities) {
      consortiumMembers.push(this.convertToConsortiumMember(consortiumOpportunity));
    }
    return consortiumMembers;
  }

  protected convertToConsortiumMember(consortiumOpportunity: ISFRawConsortiumMemberData): SFConsortiumMemberData {
    const consortiumMember: SFConsortiumMemberData = new SFConsortiumMemberData();
    consortiumMember.name = consortiumOpportunity.Account.Public_Display_Name__c;
    consortiumMember.salesforceId = consortiumOpportunity.AccountId;
    return consortiumMember;
  }
}
