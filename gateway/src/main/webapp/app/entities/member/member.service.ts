import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { BehaviorSubject, EMPTY, Observable, of, Subject, throwError, combineLatest } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
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
  SFConsortiumMemberData,
  ISFConsortiumMemberData
} from 'app/shared/model/salesforce-member-data.model';
import {
  ISFMemberContactUpdate,
  ISFRawMemberContact,
  ISFRawMemberContacts,
  SFMemberContact
} from 'app/shared/model/salesforce-member-contact.model';
import { ISFRawMemberOrgIds, SFMemberOrgIds } from 'app/shared/model/salesforce-member-org-id.model';
import { ISFMemberUpdate } from 'app/shared/model/salesforce-member-update.model';
import { ISFNewConsortiumMember } from 'app/shared/model/salesforce-new-consortium-member.model';
import { ISFCountry } from 'app/shared/model/salesforce-country.model';

type EntityResponseType = HttpResponse<IMSMember>;
type EntityArrayResponseType = HttpResponse<IMSMember[]>;
type SalesforceDataResponseType = HttpResponse<ISFRawMemberData>;
type SalesforceContactsResponseType = HttpResponse<ISFRawMemberContacts>;
type SalesforceOrgIdResponseType = HttpResponse<ISFRawMemberOrgIds>;

@Injectable({ providedIn: 'root' })
export class MSMemberService {
  public resourceUrl = SERVER_API_URL + 'services/memberservice/api';
  public orgNameMap: any;
  public memberData = new BehaviorSubject<ISFMemberData>(undefined);
  public managedMember = new BehaviorSubject<string | null>(null);
  public fetchingMemberDataState = new BehaviorSubject<boolean>(undefined);
  public stopFetchingMemberData = new Subject();
  private countries = new BehaviorSubject<ISFCountry[]>(undefined);

  constructor(protected http: HttpClient) {
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

  getMember(salesforceId: string): Observable<SFMemberData> {
    return this.http.get<ISFRawMemberData>(`${this.resourceUrl}/members/${salesforceId}/member-details`, { observe: 'response' }).pipe(
      catchError(err => {
        return of(err);
      }),
      map((res: SalesforceDataResponseType) => this.convertToSalesforceMemberData(res)),
      switchMap(value => {
        if (value && !value.id) {
          return throwError(value);
        } else {
          return of(value);
        }
      })
    );
  }

  addConsortiumMember(consortiumMember: ISFNewConsortiumMember): Observable<Boolean> {
    return this.http
      .post<ISFMemberContactUpdate>(`${this.resourceUrl}/members/add-consortium-member`, consortiumMember, { observe: 'response' })
      .pipe(
        map((res: HttpResponse<any>) => res.status === 200),
        catchError(err => {
          return throwError(err);
        })
      );
  }

  removeConsortiumMember(consortiumMember: ISFConsortiumMemberData): Observable<Boolean> {
    return this.http
      .post<ISFMemberContactUpdate>(`${this.resourceUrl}/members/remove-consortium-member`, consortiumMember, { observe: 'response' })
      .pipe(
        map((res: HttpResponse<any>) => res.status === 200),
        catchError(err => {
          return throwError(err);
        })
      );
  }

  getMemberContacts(salesforceId: string): Observable<SFMemberContact[]> {
    return this.http.get<ISFRawMemberContacts>(`${this.resourceUrl}/members/${salesforceId}/member-contacts`, { observe: 'response' }).pipe(
      takeUntil(this.stopFetchingMemberData),
      map((res: SalesforceContactsResponseType) => this.convertToSalesforceMemberContacts(res)),
      tap(res => this.memberData.next({ ...this.memberData.value, contacts: res })),
      catchError(err => {
        return of(err);
      })
    );
  }

  updateContact(contact: ISFMemberContactUpdate, salesforceId: string): Observable<Boolean> {
    return this.http
      .post<ISFMemberContactUpdate>(`${this.resourceUrl}/members/${salesforceId}/contact-update`, contact, { observe: 'response' })
      .pipe(
        map((res: HttpResponse<any>) => res.status === 200),
        catchError(err => {
          return throwError(err);
        })
      );
  }

  getMemberOrgIds(salesforceId: string): Observable<SFMemberOrgIds> {
    return this.http.get<ISFRawMemberOrgIds>(`${this.resourceUrl}/members/${salesforceId}/member-org-ids`, { observe: 'response' }).pipe(
      takeUntil(this.stopFetchingMemberData),
      map((res: SalesforceOrgIdResponseType) => this.convertToMemberOrgIds(res)),
      tap(res => this.memberData.next({ ...this.memberData.value, orgIds: res })),
      catchError(err => {
        return of(err);
      })
    );
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/members/${id}`, { observe: 'response' });
  }

  updateMemberDetails(memberDetails: ISFMemberUpdate, salesforceId: string): Observable<ISFMemberUpdate> {
    return this.http.put(`${this.resourceUrl}/members/${salesforceId}/member-details`, memberDetails);
  }

  getConsortiaLeadName(consortiaLeadId: string): Observable<EntityResponseType> {
    if (consortiaLeadId) {
      return this.find(consortiaLeadId).pipe(
        tap(r => {
          if (r && r.body) {
            this.memberData.next({ ...this.memberData.value, consortiumLeadName: r.body.clientName });
          }
        })
      );
    }
    return of(null);
  }

  getIsConsortiumLead(salesforceId: string): Observable<EntityResponseType | never> {
    if (salesforceId) {
      return this.find(salesforceId).pipe(
        tap(r => {
          if (r && r.body) {
            const { isConsortiumLead } = r.body;
            this.memberData.next({ ...this.memberData.value, isConsortiumLead });
          }
        })
      );
    }
    return of(null);
  }

  getCountries(): Observable<ISFCountry[]> {
    if (!this.countries.value) {
      return this.fetchCountries();
    }
    return this.countries.asObservable();
  }

  fetchCountries(): Observable<ISFCountry[]> {
    return this.http.get(`${this.resourceUrl}/countries`, { observe: 'response' }).pipe(
      catchError(error => {
        return of('An error occurred:', error);
      }),
      map((res: HttpResponse<ISFCountry[]>) => {
        if (res.status === 200) {
          this.countries.next(res.body);
          return res.body;
        } else {
          console.error('Request failed:', res);
        }
      })
    );
  }

  fetchMemberData(salesforceId: string) {
    if (this.memberData.value && this.managedMember.value !== this.memberData.value.id) {
      this.stopFetchingMemberData.next();
      this.fetchingMemberDataState.next(false);
    }

    if (!this.fetchingMemberDataState.value) {
      if (!this.memberData.value || this.memberData.value.id !== this.managedMember.value) {
        this.fetchingMemberDataState.next(true);
        this.getMember(salesforceId)
          .pipe(
            switchMap(res => {
              this.memberData.next(res);
              return combineLatest([
                this.getMemberContacts(salesforceId),
                this.getMemberOrgIds(salesforceId),
                this.getConsortiaLeadName(res.consortiaLeadId),
                this.getIsConsortiumLead(salesforceId)
              ]);
            }),
            tap(res => {
              this.fetchingMemberDataState.next(false);
            }),
            catchError(() => {
              this.memberData.next(null);
              this.fetchingMemberDataState.next(false);
              return EMPTY;
            })
          )
          .subscribe();
      }
    }
  }

  getManagedMember(): Observable<string | null> {
    return this.managedMember.asObservable();
  }

  setManagedMember(value: string | null) {
    this.managedMember.next(value);
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

  protected convertToSalesforceMemberData(res: SalesforceDataResponseType): SFMemberData {
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
        consortiumMembers: res.body.consortiumOpportunities ? this.convertToConsortiumMembers(res.body.consortiumOpportunities) : null,
        billingAddress: res.body.BillingAddress,
        trademarkLicense: res.body.Trademark_License__c
      };
    } else {
      return new SFMemberData();
    }
  }

  protected convertToSalesforceMemberContacts(res: SalesforceContactsResponseType): SFMemberContact[] {
    const contacts = {};
    if (res.body && res.body.records.length > 0) {
      for (const contact of res.body.records) {
        // Merge contacts with different roles to a single entry if they have a matching email
        if (!contacts[contact.Contact_Curr_Email__c]) {
          contacts[contact.Contact_Curr_Email__c] = this.convertToSalesforceMemberContact(contact);
        }
        if (contact.Voting_Contact__c) {
          contacts[contact.Contact_Curr_Email__c].memberOrgRole.push('Voting contact');
        }
        contacts[contact.Contact_Curr_Email__c].memberOrgRole.unshift(contact.Member_Org_Role__c);
      }
      return Object.values(contacts);
    } else {
      return null;
    }
  }

  protected convertToSalesforceMemberContact(res: ISFRawMemberContact): SFMemberContact {
    return {
      ...new SFMemberContact(),
      memberId: res.Organization__c,
      votingContact: res.Voting_Contact__c,
      name: res.Name,
      contactEmail: res.Contact_Curr_Email__c,
      phone: res.Phone,
      title: res.Title
    };
  }

  protected convertToMemberOrgIds(res: SalesforceOrgIdResponseType): SFMemberOrgIds {
    if (res.body && res.body.records.length > 0) {
      const ids = res.body.records;
      const ROR = [],
        GRID = [],
        Ringgold = [],
        Fundref = [];
      for (var i = 0; i < ids.length; i++) {
        if (ids[i].Identifier_Type__c === 'ROR') {
          ROR.push(ids[i].Name);
        }
        if (ids[i].Identifier_Type__c === 'GRID') {
          GRID.push(ids[i].Name);
        }
        if (ids[i].Identifier_Type__c === 'Ringgold ID') {
          Ringgold.push(ids[i].Name);
        }
        if (ids[i].Identifier_Type__c === 'FundRef ID') {
          Fundref.push(ids[i].Name);
        }
      }
      return {
        ...new SFMemberOrgIds(),
        ROR,
        GRID,
        Ringgold,
        Fundref
      };
    } else {
      return null;
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
    consortiumMember.orgName = consortiumOpportunity.Account.Public_Display_Name__c;
    consortiumMember.salesforceId = consortiumOpportunity.AccountId;
    return consortiumMember;
  }
}
