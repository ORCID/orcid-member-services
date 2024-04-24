import { Injectable } from '@angular/core'
import {
  BehaviorSubject,
  Observable,
  of,
  map,
  catchError,
  Subject,
  switchMap,
  throwError,
  takeUntil,
  tap,
  combineLatest,
  EMPTY,
  filter,
} from 'rxjs'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { IMember } from '../model/member.model'
import * as moment from 'moment'
import { createRequestOption } from 'src/app/shared/request-util'
import { IMemberPage, MemberPage } from '../model/member-page.model'
import {
  ISFMemberData,
  ISFRawConsortiumMemberData,
  ISFRawMemberData,
  SFConsortiumMemberData,
  SFMemberData,
} from '../model/salesforce-member-data.model'
import { ISFCountry } from '../model/salesforce-country.model'
import {
  ISFMemberContactUpdate,
  ISFRawMemberContact,
  ISFRawMemberContacts,
  SFMemberContact,
} from '../model/salesforce-member-contact.model'
import { ISFRawMemberOrgIds, SFMemberOrgIds } from '../model/salesforce-member-org-id.model'

@Injectable({ providedIn: 'root' })
export class MemberService {
  constructor(protected http: HttpClient) {}

  public resourceUrl = '/services/memberservice/api'
  public managedMember = new BehaviorSubject<string | null>(null)

  private memberData = new BehaviorSubject<ISFMemberData | undefined | null>(undefined)
  private fetchingMemberDataState = false
  public stopFetchingMemberData = new Subject()
  private countries = new BehaviorSubject<ISFCountry[] | undefined>(undefined)

  find(id: string): Observable<IMember> {
    return this.http.get<IMember>(`${this.resourceUrl}/members/${id}`).pipe(
      map((res: IMember) => this.convertDateFromServer(res)),
      catchError((err) => {
        return of(err)
      })
    )
  }

  create(msMember: IMember): Observable<IMember> {
    const copy = this.convertDateFromClient(msMember)
    return this.http
      .post<IMember>(`${this.resourceUrl}/members`, copy)
      .pipe(map((res: IMember) => this.convertDateFromServer(res)))
  }

  update(msMember: IMember): Observable<IMember> {
    const copy = this.convertDateFromClient(msMember)
    return this.http
      .put<IMember>(`${this.resourceUrl}/members`, copy)
      .pipe(map((res: IMember) => this.convertDateFromServer(res)))
  }

  validate(member: IMember): Observable<{ valid: boolean; errors?: string[] }> {
    const copy = this.convertDateFromClient(member)
    return this.http.post<{ valid: boolean; errors: string[] }>(`${this.resourceUrl}/members/validate`, copy)
  }

  getAllMembers(): Observable<IMember[]> {
    return this.http
      .get<IMember[]>(`${this.resourceUrl}/members/list/all`)
      .pipe(map((res: IMember[]) => this.convertMembersArrayFromServer(res)))
  }

  query(req?: any): Observable<IMemberPage | null> {
    const options = createRequestOption(req)
    return this.http
      .get<IMember[]>(this.resourceUrl + '/members', { params: options, observe: 'response' })
      .pipe(map((res: HttpResponse<IMember[]>) => this.convertToMemberPage(res)))
  }

  getManagedMember(): Observable<string | null> {
    return this.managedMember.asObservable()
  }

  setManagedMember(value: string | null) {
    this.managedMember.next(value)
  }

  protected convertDateFromClient(member: IMember): IMember {
    const copy: IMember = Object.assign({}, member, {
      createdDate: member.createdDate != null && member.createdDate.isValid() ? member.createdDate.toJSON() : null,
      lastModifiedDate:
        member.lastModifiedDate != null && member.lastModifiedDate.isValid() ? member.lastModifiedDate.toJSON() : null,
    })
    return copy
  }

  protected convertDateFromServer(member: IMember): IMember {
    if (member) {
      member.createdDate = member.createdDate != null ? moment(member.createdDate) : undefined
      member.lastModifiedDate = member.lastModifiedDate != null ? moment(member.lastModifiedDate) : undefined
    }
    return member
  }

  protected convertMembersArrayFromServer(members: IMember[]): IMember[] {
    if (members) {
      members.forEach((member: IMember) => {
        member.createdDate = member.createdDate != null ? moment(member.createdDate) : null
        member.lastModifiedDate = member.lastModifiedDate != null ? moment(member.lastModifiedDate) : null
      })
    }
    return members
  }

  protected convertToMemberPage(res: HttpResponse<IMember[]>): IMemberPage | null {
    if (res.body) {
      res.body.forEach((member: IMember) => {
        member.createdDate = member.createdDate ? moment(member.createdDate) : undefined
        member.lastModifiedDate = member.lastModifiedDate ? moment(member.lastModifiedDate) : undefined
      })
      const totalCount: string | null = res.headers.get('X-Total-Count')
      if (totalCount) {
        const userPage = new MemberPage(res.body, parseInt(totalCount, 10))
        return userPage
      }
    }
    return null
  }

  getMemberData(salesforceId?: string, force?: boolean): Observable<ISFMemberData | undefined | null> {
    if (force) {
      this.stopFetchingMemberData.next(true)
    }

    if (salesforceId && (!this.memberData.value || this.memberData.value.id !== salesforceId || force)) {
      this.fetchMemberData(salesforceId)
    }

    return this.memberData.asObservable()
  }

  private fetchMemberData(salesforceId: string) {
    this.fetchingMemberDataState = true
    this.getSFMemberData(salesforceId)
      .pipe(
        switchMap((res) => {
          this.memberData.next(res)
          return combineLatest([
            this.getMemberContacts(salesforceId),
            this.getMemberOrgIds(salesforceId),
            this.getConsortiaLeadName(res.consortiaLeadId!),
            this.getIsConsortiumLead(salesforceId),
          ])
        }),
        tap((res) => {
          this.fetchingMemberDataState = false
        }),
        catchError(() => {
          this.memberData.next(null)
          this.fetchingMemberDataState = false
          return EMPTY
        })
      )
      .subscribe()
  }

  getMemberContacts(salesforceId: string): Observable<SFMemberContact[]> {
    return this.http
      .get<ISFRawMemberContacts>(`${this.resourceUrl}/members/${salesforceId}/member-contacts`, { observe: 'response' })
      .pipe(
        takeUntil(this.stopFetchingMemberData),
        map((res: HttpResponse<ISFRawMemberContacts>) => this.convertToSalesforceMemberContacts(res)),
        tap((res) => this.memberData.next({ ...this.memberData.value, contacts: res })),
        catchError((err) => {
          return of(err)
        })
      )
  }

  getSFMemberData(salesforceId: string): Observable<SFMemberData> {
    return this.http
      .get<ISFRawMemberData>(`${this.resourceUrl}/members/${salesforceId}/member-details`, { observe: 'response' })
      .pipe(
        catchError((err) => {
          return of(err)
        }),
        map((res: HttpResponse<ISFRawMemberData>) => this.convertToSalesforceMemberData(res)),
        switchMap((value) => {
          if (value && !value.id) {
            return throwError(value)
          } else {
            return of(value)
          }
        })
      )
  }

  getMemberOrgIds(salesforceId: string): Observable<SFMemberOrgIds> {
    return this.http
      .get<ISFRawMemberOrgIds>(`${this.resourceUrl}/members/${salesforceId}/member-org-ids`, { observe: 'response' })
      .pipe(
        takeUntil(this.stopFetchingMemberData),
        map((res: HttpResponse<ISFRawMemberOrgIds>) => this.convertToMemberOrgIds(res)),
        filter((res): res is SFMemberOrgIds => !!res),
        tap((res) => this.memberData.next({ ...this.memberData.value, orgIds: res })),
        catchError((err) => {
          return of(err)
        })
      )
  }

  getConsortiaLeadName(consortiaLeadId: string): Observable<IMember | null> {
    if (consortiaLeadId) {
      return this.find(consortiaLeadId).pipe(
        tap((member) => {
          if (member) {
            this.memberData.next({ ...this.memberData.value, consortiumLeadName: member.clientName })
          }
        })
      )
    }
    return of(null)
  }

  getIsConsortiumLead(salesforceId: string): Observable<IMember | never | null> {
    if (salesforceId) {
      return this.find(salesforceId).pipe(
        tap((member) => {
          if (member) {
            const { isConsortiumLead } = member
            this.memberData.next({ ...this.memberData.value, isConsortiumLead })
          }
        })
      )
    }
    return of(null)
  }

  updateContact(contact: ISFMemberContactUpdate, salesforceId: string): Observable<Boolean> {
    return this.http
      .post<ISFMemberContactUpdate>(`${this.resourceUrl}/members/${salesforceId}/contact-update`, contact, {
        observe: 'response',
      })
      .pipe(
        map((res: HttpResponse<any>) => res.status === 200),
        catchError((err) => {
          return throwError(err)
        })
      )
  }

  private convertToSalesforceMemberData(res: HttpResponse<ISFRawMemberData>): SFMemberData {
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
        consortiumMembers: res.body.consortiumOpportunities
          ? this.convertToConsortiumMembers(res.body.consortiumOpportunities)
          : undefined,
        billingAddress: res.body.BillingAddress,
        trademarkLicense: res.body.Trademark_License__c,
      }
    } else {
      return new SFMemberData()
    }
  }

  private convertToConsortiumMembers(consortiumOpportunities: ISFRawConsortiumMemberData[]): SFConsortiumMemberData[] {
    const consortiumMembers: SFConsortiumMemberData[] = []
    for (const consortiumOpportunity of consortiumOpportunities) {
      consortiumMembers.push(this.convertToConsortiumMember(consortiumOpportunity))
    }
    return consortiumMembers
  }

  private convertToConsortiumMember(consortiumOpportunity: ISFRawConsortiumMemberData): SFConsortiumMemberData {
    const consortiumMember: SFConsortiumMemberData = new SFConsortiumMemberData()
    consortiumMember.orgName = consortiumOpportunity?.Account?.Public_Display_Name__c
    consortiumMember.salesforceId = consortiumOpportunity.AccountId
    return consortiumMember
  }

  private convertToSalesforceMemberContacts(res: HttpResponse<ISFRawMemberContacts>): SFMemberContact[] {
    const contacts: { [email: string]: SFMemberContact } = {}
    if (res.body && res.body.records && res.body.records.length > 0) {
      for (const contact of res.body.records) {
        // Merge contacts with different roles to a single entry if they have a matching email
        if (contact.Contact_Curr_Email__c) {
          if (!contacts[contact.Contact_Curr_Email__c]) {
            contacts[contact.Contact_Curr_Email__c] = this.convertToSalesforceMemberContact(contact)
          }
          if (contact.Voting_Contact__c) {
            contacts[contact.Contact_Curr_Email__c].memberOrgRole!.push('Voting contact')
          }
          if (contact.Member_Org_Role__c) {
            contacts[contact.Contact_Curr_Email__c].memberOrgRole!.unshift(contact.Member_Org_Role__c)
          }
        }
      }
      return Object.values(contacts)
    } else {
      return []
    }
  }

  private convertToSalesforceMemberContact(res: ISFRawMemberContact): SFMemberContact {
    return {
      ...new SFMemberContact(),
      memberId: res.Organization__c,
      votingContact: res.Voting_Contact__c,
      name: res.Name,
      contactEmail: res.Contact_Curr_Email__c,
      phone: res.Phone,
      title: res.Title,
    }
  }

  private convertToMemberOrgIds(res: HttpResponse<ISFRawMemberOrgIds>): SFMemberOrgIds | null {
    if (res.body && res.body.records && res.body.records.length > 0) {
      const ids = res.body.records
      const ROR = [],
        GRID = [],
        Ringgold = [],
        Fundref = []
      for (let i = 0; i < ids.length; i++) {
        if (ids[i].Identifier_Type__c === 'ROR') {
          ROR.push(ids[i].Name)
        }
        if (ids[i].Identifier_Type__c === 'GRID') {
          GRID.push(ids[i].Name)
        }
        if (ids[i].Identifier_Type__c === 'Ringgold ID') {
          Ringgold.push(ids[i].Name)
        }
        if (ids[i].Identifier_Type__c === 'FundRef ID') {
          Fundref.push(ids[i].Name)
        }
      }
      return {
        ...new SFMemberOrgIds(),
        ROR,
        GRID,
        Ringgold,
        Fundref,
      }
    } else {
      return null
    }
  }
}
