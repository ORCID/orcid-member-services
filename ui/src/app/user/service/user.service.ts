import { Injectable } from '@angular/core'
import { HttpClient, HttpResponse, HttpHeaders, HttpParams } from '@angular/common/http'
import { Observable } from 'rxjs'
import * as moment from 'moment'
import { filter, map } from 'rxjs/operators'

import { User, UserAuthorities } from '../model/user.model'
import { createRequestOption } from '../../shared/request-util'
import { UserValidation } from '../model/user-validation.model'
import { IUserPage, UserPage } from '../model/user-page.model'

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

  findBySalesForceId(salesforceId: string | null, req?: any): Observable<IUserPage | null> {
    const options = createRequestOption(req)
    return this.http
      .get<User[]>(`${this.resourceUrl}/salesforce/${salesforceId}/p`, { params: options, observe: 'response' })
      .pipe(map((res: HttpResponse<User[]>) => this.convertToUserPage(res)))
  }

  query(req?: any): Observable<IUserPage | null> {
    const options = createRequestOption(req)
    return this.http
      .get<User[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: HttpResponse<User[]>) => this.convertToUserPage(res)))
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

  protected convertToUserPage(res: HttpResponse<User[]>): UserPage | null {
    if (res.body) {
      res.body.forEach((user: User) => {
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
      const totalCount: string | null = res.headers.get('X-Total-Count')

      if (totalCount) {
        const userPage = new UserPage(res.body, parseInt(totalCount, 10))
        return userPage
      }
    }
    return null
  }
}
