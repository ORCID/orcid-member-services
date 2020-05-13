/* tslint:disable max-line-length */
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { take, map } from 'rxjs/operators';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { AssertionService } from 'app/entities/AssertionService/assertion/assertion.service';
import { IAssertion, Assertion, AffiliationSection } from 'app/shared/model/AssertionService/assertion.model';

describe('Service Tests', () => {
  describe('Assertion Service', () => {
    let injector: TestBed;
    let service: AssertionService;
    let httpMock: HttpTestingController;
    let elemDefault: IAssertion;
    let expectedResult;
    let currentDate: moment.Moment;
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule]
      });
      expectedResult = {};
      injector = getTestBed();
      service = injector.get(AssertionService);
      httpMock = injector.get(HttpTestingController);
      currentDate = moment();

      elemDefault = new Assertion(
        'ID',
        'AAAAAAA',
        AffiliationSection.EMPLOYMENT,
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        'AAAAAAA',
        currentDate,
        currentDate,
        currentDate,
        false,
        'AAAAAAA'
      );
    });

    describe('Service methods', () => {
      it('should find an element', async () => {
        const returnedFromService = Object.assign(
          {
            created: currentDate.format(DATE_TIME_FORMAT),
            modified: currentDate.format(DATE_TIME_FORMAT),
            deletedFromORCID: currentDate.format(DATE_TIME_FORMAT)
          },
          elemDefault
        );
        service
          .find('123')
          .pipe(take(1))
          .subscribe(resp => (expectedResult = resp));

        const req = httpMock.expectOne({ method: 'GET' });
        req.flush(returnedFromService);
        expect(expectedResult).toMatchObject({ body: elemDefault });
      });

      it('should create a Assertion', async () => {
        const returnedFromService = Object.assign(
          {
            id: 'ID',
            created: currentDate.format(DATE_TIME_FORMAT),
            modified: currentDate.format(DATE_TIME_FORMAT),
            deletedFromORCID: currentDate.format(DATE_TIME_FORMAT)
          },
          elemDefault
        );
        const expected = Object.assign(
          {
            created: currentDate,
            modified: currentDate,
            deletedFromORCID: currentDate
          },
          returnedFromService
        );
        service
          .create(new Assertion(null))
          .pipe(take(1))
          .subscribe(resp => (expectedResult = resp));
        const req = httpMock.expectOne({ method: 'POST' });
        req.flush(returnedFromService);
        expect(expectedResult).toMatchObject({ body: expected });
      });

      it('should update a Assertion', async () => {
        const returnedFromService = Object.assign(
          {
            email: 'BBBBBB',
            affiliationSection: 'BBBBBB',
            departmentName: 'BBBBBB',
            roleTitle: 'BBBBBB',
            url: 'BBBBBB',
            startYear: 'BBBBBB',
            startMonth: 'BBBBBB',
            startDay: 'BBBBBB',
            endYear: 'BBBBBB',
            endMonth: 'BBBBBB',
            endDay: 'BBBBBB',
            orgName: 'BBBBBB',
            orgCountry: 'BBBBBB',
            orgCity: 'BBBBBB',
            orgRegion: 'BBBBBB',
            disambiguatedOrgId: 'BBBBBB',
            disambiguationSource: 'BBBBBB',
            externalId: 'BBBBBB',
            externalIdType: 'BBBBBB',
            externalIdUrl: 'BBBBBB',
            putCode: 'BBBBBB',
            created: currentDate.format(DATE_TIME_FORMAT),
            modified: currentDate.format(DATE_TIME_FORMAT),
            deletedFromORCID: currentDate.format(DATE_TIME_FORMAT),
            sent: true,
            adminId: 'BBBBBB'
          },
          elemDefault
        );

        const expected = Object.assign(
          {
            created: currentDate,
            modified: currentDate,
            deletedFromORCID: currentDate
          },
          returnedFromService
        );
        service
          .update(expected)
          .pipe(take(1))
          .subscribe(resp => (expectedResult = resp));
        const req = httpMock.expectOne({ method: 'PUT' });
        req.flush(returnedFromService);
        expect(expectedResult).toMatchObject({ body: expected });
      });

      it('should return a list of Assertion', async () => {
        const returnedFromService = Object.assign(
          {
            email: 'BBBBBB',
            affiliationSection: 'BBBBBB',
            departmentName: 'BBBBBB',
            roleTitle: 'BBBBBB',
            url: 'BBBBBB',
            startYear: 'BBBBBB',
            startMonth: 'BBBBBB',
            startDay: 'BBBBBB',
            endYear: 'BBBBBB',
            endMonth: 'BBBBBB',
            endDay: 'BBBBBB',
            orgName: 'BBBBBB',
            orgCountry: 'BBBBBB',
            orgCity: 'BBBBBB',
            orgRegion: 'BBBBBB',
            disambiguatedOrgId: 'BBBBBB',
            disambiguationSource: 'BBBBBB',
            externalId: 'BBBBBB',
            externalIdType: 'BBBBBB',
            externalIdUrl: 'BBBBBB',
            putCode: 'BBBBBB',
            created: currentDate.format(DATE_TIME_FORMAT),
            modified: currentDate.format(DATE_TIME_FORMAT),
            deletedFromORCID: currentDate.format(DATE_TIME_FORMAT),
            sent: true,
            adminId: 'BBBBBB'
          },
          elemDefault
        );
        const expected = Object.assign(
          {
            created: currentDate,
            modified: currentDate,
            deletedFromORCID: currentDate
          },
          returnedFromService
        );
        service
          .query(expected)
          .pipe(
            take(1),
            map(resp => resp.body)
          )
          .subscribe(body => (expectedResult = body));
        const req = httpMock.expectOne({ method: 'GET' });
        req.flush([returnedFromService]);
        httpMock.verify();
        expect(expectedResult).toContainEqual(expected);
      });

      it('should delete a Assertion', async () => {
        const rxPromise = service.delete('123').subscribe(resp => (expectedResult = resp.ok));

        const req = httpMock.expectOne({ method: 'DELETE' });
        req.flush({ status: 200 });
        expect(expectedResult);
      });
    });

    afterEach(() => {
      httpMock.verify();
    });
  });
});
