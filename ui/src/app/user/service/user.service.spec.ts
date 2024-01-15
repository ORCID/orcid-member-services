/* tslint:disable max-line-length */
import { TestBed, getTestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { take, map } from 'rxjs/operators'
import * as moment from 'moment'
import { DATE_TIME_FORMAT } from '../../app.constants'

import { IUser, User } from '../model/user.model'
import { UserService } from './user.service'

describe('Service Tests', () => {
  describe('Member service users service', () => {
    let injector: TestBed
    let service: UserService
    let httpMock: HttpTestingController
    let elemDefault: User
    let expectedResult: any | null = null
    let currentDate: moment.Moment
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
      })
      expectedResult = {}
      injector = getTestBed()
      service = injector.get(UserService)
      httpMock = injector.get(HttpTestingController)
      currentDate = moment()

      elemDefault = new User(
        'ID',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        false,
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'Member name',
        currentDate,
        'AAAAAAA',
        false,
        false,
        currentDate
      )
    })

    describe('Service methods', () => {
      it('should create a user', async () => {
        const returnedFromService = Object.assign(
          {
            id: 'ID',
            createdDate: currentDate.format(DATE_TIME_FORMAT),
            lastModifiedDate: currentDate.format(DATE_TIME_FORMAT),
          },
          elemDefault
        )
        const expected = Object.assign(
          {
            createdDate: currentDate,
            lastModifiedDate: currentDate,
          },
          returnedFromService
        )
        service
          .create(new User(undefined))
          .pipe(take(1))
          .subscribe((resp) => (expectedResult = resp))
        const req = httpMock.expectOne({ method: 'POST' })
        req.flush(returnedFromService)
        expect(expectedResult).toEqual(expected)
      })

      it('should update a user', async () => {
        const returnedFromService = Object.assign(
          {
            login: 'BBBBBB',
            email: 'BBBBBB',
            password: 'BBBBBB',
            firstName: 'BBBBBB',
            lastName: 'BBBBBB',
            mainContact: true,
            salesforceId: 'BBBBBB',
            parentSalesforceId: 'BBBBBB',
            activated: false,
            createdBy: 'BBBBBB',
            createdDate: currentDate.format(DATE_TIME_FORMAT),
            lastModifiedBy: 'BBBBBB',
            lastModifiedDate: currentDate.format(DATE_TIME_FORMAT),
          },
          elemDefault
        )

        const expected = Object.assign(
          {
            createdDate: currentDate,
            lastModifiedDate: currentDate,
          },
          returnedFromService
        )
        service
          .update(expected)
          .pipe(take(1))
          .subscribe((resp) => (expectedResult = resp))
        const req = httpMock.expectOne({ method: 'PUT' })
        req.flush(returnedFromService)
        expect(expectedResult).toEqual(expected)
      })

      it('should return a list of MSUserService', async () => {
        const returnedFromService = Object.assign(
          {
            login: 'BBBBBB',
            email: 'BBBBBB',
            password: 'BBBBBB',
            firstName: 'BBBBBB',
            lastName: 'BBBBBB',
            mainContact: true,
            salesforceId: 'BBBBBB',
            parentSalesforceId: 'BBBBBB',
            activated: false,
            createdBy: 'BBBBBB',
            createdDate: currentDate.format(DATE_TIME_FORMAT),
            lastModifiedBy: 'BBBBBB',
            lastModifiedDate: currentDate.format(DATE_TIME_FORMAT),
          },
          elemDefault
        )
        const expected = Object.assign(
          {
            createdDate: currentDate,
            lastModifiedDate: currentDate,
          },
          returnedFromService
        )
        service
          .query(expected)
          .pipe(take(1))
          .subscribe((body) => (expectedResult = body))
        const req = httpMock.expectOne({ method: 'GET' })
        req.flush([returnedFromService])
        httpMock.verify()
        const expectedResultList = [expectedResult]
        expect(expectedResult).toContain(expected)
      })
    })

    afterEach(() => {
      httpMock.verify()
    })
  })
})
