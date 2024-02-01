import { Injectable } from '@angular/core'
import { BehaviorSubject, Observable, of, map, catchError } from 'rxjs'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { IMember } from '../model/member.model'
import * as moment from 'moment'

type EntityResponseType = HttpResponse<IMember>
type EntityArrayResponseType = HttpResponse<IMember[]>;

@Injectable({ providedIn: 'root' })
export class MemberService {
  constructor(protected http: HttpClient) {}

  public resourceUrl = '/services/memberservice/api'
  public managedMember = new BehaviorSubject<string | null>(null)

  find(id: string): Observable<IMember | null> {
    return this.http
      .get<IMember>(`${this.resourceUrl}/members/${id}`, {
        observe: 'response',
      })
      .pipe(
        map((res: EntityResponseType) => this.convertDateFromServer(res)),
        catchError((err) => {
          return of(err)
        })
      )
  }

  getAllMembers(): Observable<EntityArrayResponseType> {
    return this.http
      .get<IMember[]>(`${this.resourceUrl}/members/list/all`, { observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  getManagedMember(): Observable<string | null> {
    return this.managedMember.asObservable()
  }

  setManagedMember(value: string | null) {
    this.managedMember.next(value)
  }

  protected convertDateFromServer(res: EntityResponseType): IMember | null {
    if (res.body) {
      res.body.createdDate = res.body.createdDate != null ? moment(res.body.createdDate) : undefined
      res.body.lastModifiedDate = res.body.lastModifiedDate != null ? moment(res.body.lastModifiedDate) : undefined
    }
    return res.body
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((member: IMember) => {
        member.createdDate = member.createdDate != null ? moment(member.createdDate) : null;
        member.lastModifiedDate = member.lastModifiedDate != null ? moment(member.lastModifiedDate) : null;
      });
    }
    return res;
  }
}
