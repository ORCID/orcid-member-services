import { Injectable } from '@angular/core'
import { BehaviorSubject, Observable, of, map, catchError } from 'rxjs'
import { HttpClient, HttpResponse } from '@angular/common/http'
import { IMember } from '../model/member.model'
import { SERVER_API_URL } from 'src/app/app.constants'
import * as moment from 'moment'

type EntityResponseType = HttpResponse<IMember>

@Injectable({ providedIn: 'root' })
export class MemberService {
  constructor(protected http: HttpClient) {}

  public resourceUrl = SERVER_API_URL + 'services/memberservice/api'
  public managedMember = new BehaviorSubject<string | null>(null)

  find(id: string): Observable<EntityResponseType> {
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

  getManagedMember(): Observable<string | null> {
    return this.managedMember.asObservable()
  }

  setManagedMember(value: string | null) {
    this.managedMember.next(value)
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdDate = res.body.createdDate != null ? moment(res.body.createdDate) : undefined
      res.body.lastModifiedDate = res.body.lastModifiedDate != null ? moment(res.body.lastModifiedDate) : undefined
    }
    return res
  }
}
