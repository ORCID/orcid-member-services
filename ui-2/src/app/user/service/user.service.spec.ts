/* tslint:disable max-line-length */
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import moment from 'moment'
import { take } from 'rxjs/operators'
import { DATE_TIME_FORMAT } from '../../app.constants'

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { User } from '../model/user.model'
import { UserService } from './user.service'

describe('Service Tests', () => {
  describe('Member service users service', () => {
    let service: UserService
    let httpMock: HttpTestingController
    let elemDefault: User
    let result: any | null = null
    let currentDate: moment.Moment
    beforeEach(() => {
      TestBed.configureTestingModule({
    imports: [],
    providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
})
      result = {}
      service = TestBed.inject(UserService)
      httpMock = TestBed.inject(HttpTestingController)
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
          .subscribe((resp) => (result = resp))
        const req = httpMock.expectOne({ method: 'POST' })
        req.flush(returnedFromService)
        expect(result).toEqual(expected)
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
          .subscribe((resp) => (result = resp))
        const req = httpMock.expectOne({ method: 'PUT' })
        req.flush(returnedFromService)
        expect(result).toEqual(expected)
      })

      it('should return a list of users', async () => {
        service
          .query({ sort: ['name,desc', 'id'] })
          .pipe(take(1))
          .subscribe((body) => (result = body))

        const req = httpMock.expectOne({ method: 'GET' })

        req.flush({
          content: [new User('123')],
          page: {
            totalElements: 100,
            number: 0,
            size: 20,
            totalPages: 5,
          },
        })

        httpMock.verify()

        expect(result).toBeTruthy()

        expect(result.content.length).toEqual(1)

        expect(result.page.totalElements).toEqual(100)
      })
    })

    afterEach(() => {
      httpMock.verify()
    })
  })
})
