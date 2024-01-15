import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse, HttpHeaders, HttpParams } from '@angular/common/http'
import { Observable } from 'rxjs'
import * as moment from 'moment'
import { map } from 'rxjs/operators'

import { User, UserAuthorities } from '../model/user.model'
import { createRequestOption } from '../../shared/request-util'
import { UserValidation } from '../model/user-validation.model'

@Injectable({ providedIn: 'root' })
export class UserService {
  public resourceUrl = '/services/userservice/api/users'
  private switchResourceUrl = '/services/userservice/api'

  constructor(protected http: HttpClient) {}

  create(user: User): Observable<User> {
    const copy = this.convertDateFromClient(user)
    return this.http.post<User>(this.resourceUrl, copy).pipe(map((res: User) => this.convertFromServer(res)))
  }

  validate(user: User): Observable<UserValidation> {
    const copy = this.convertDateFromClient(user)
    return this.http.post<UserValidation>(this.resourceUrl + '/validate', copy)
  }

  upload(user: User): Observable<User> {
    const copy = this.convertDateFromClient(user)
    return this.http.post<User>(this.resourceUrl, copy).pipe(map((res: User) => this.convertFromServer(res)))
  }

  update(user: User): Observable<User> {
    const copy = this.convertDateFromClient(user)
    return this.http.put<User>(this.resourceUrl, copy).pipe(map((res: User) => this.convertFromServer(res)))
  }

  sendActivate(user: User): Observable<User> {
    const copy = this.convertDateFromClient(user)
    return this.http.post<User>(`${this.resourceUrl}/${user.id}/sendActivate`, copy)
  }

  find(email: string): Observable<User> {
    return this.http.get<User>(`${this.resourceUrl}/${email}`).pipe(map((res: User) => this.convertFromServer(res)))
  }

  hasOwner(salesforceId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.resourceUrl}/${salesforceId}/owner`)
  }

  findBySalesForceId(salesforceId: string, req?: any): Observable<User[]> {
    const options = createRequestOption(req)
    return this.http
      .get<User[]>(`${this.resourceUrl}/salesforce/${salesforceId}/p`, { params: options })
      .pipe(map((res: User[]) => this.convertArrayFromServer(res)))
  }

  query(req?: any): Observable<User[]> {
    const options = createRequestOption(req)
    return this.http
      .get<User[]>(this.resourceUrl, { params: options })
      .pipe(map((res: User[]) => this.convertArrayFromServer(res)))
  }

  delete(id: string): Observable<HttpResponse<any>> {
    return this.http.delete<any>(`${this.resourceUrl}/${id}`)
  }

  switchUser(username: string): Observable<any> {
    const formData = new FormData()
    formData.set('username', username)
    return this.http.post(`${this.switchResourceUrl}/switch_user`, formData, {
      headers: new HttpHeaders().set('Accept', 'text/html'),
      withCredentials: true,
      responseType: 'text',
    })
  }

  protected convertDateFromClient(user: User): User {
    const copy: User = Object.assign({}, user, {
      createdDate: user.createdDate != null && user.createdDate.isValid() ? user.createdDate.toJSON() : null,
      lastModifiedDate:
        user.lastModifiedDate != null && user.lastModifiedDate.isValid() ? user.lastModifiedDate.toJSON() : null,
    })
    return copy
  }

  protected convertFromServer(res: User): User {
    if (res) {
      res.createdDate = res.createdDate != null ? moment(res.createdDate) : null
      res.lastModifiedDate = res.lastModifiedDate != null ? moment(res.lastModifiedDate) : null
      res.isAdmin = false
      if (res.authorities != null) {
        res.authorities.forEach(function (userRole) {
          if (userRole === UserAuthorities.ROLE_ADMIN) {
            res.isAdmin = true
          }
        })
      }
    }
    return res
  }

  protected convertArrayFromServer(res: User[]): User[] {
    if (res) {
      res.forEach((user: User) => {
        user.createdDate = user.createdDate != null ? moment(user.createdDate) : null
        user.lastModifiedDate = user.lastModifiedDate != null ? moment(user.lastModifiedDate) : null
        user.isAdmin = false
        if (user.authorities != null) {
          user.authorities.forEach(function (userRole) {
            if (userRole === UserAuthorities.ROLE_ADMIN) {
              user.isAdmin = true
            }
          })
        }
      })
    }
    return res
  }
}
