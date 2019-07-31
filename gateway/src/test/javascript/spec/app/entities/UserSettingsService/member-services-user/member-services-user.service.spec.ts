/* tslint:disable max-line-length */
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { take, map } from 'rxjs/operators';
import { MemberServicesUserService } from 'app/entities/UserSettingsService/member-services-user/member-services-user.service';
import { IMemberServicesUser, MemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';

describe('Service Tests', () => {
  describe('MemberServicesUser Service', () => {
    let injector: TestBed;
    let service: MemberServicesUserService;
    let httpMock: HttpTestingController;
    let elemDefault: IMemberServicesUser;
    let expectedResult;
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule]
      });
      expectedResult = {};
      injector = getTestBed();
      service = injector.get(MemberServicesUserService);
      httpMock = injector.get(HttpTestingController);

      elemDefault = new MemberServicesUser('ID', 'AAAAAAA', 'AAAAAAA', 'AAAAAAA', false, false, false, 'AAAAAAA');
    });

    describe('Service methods', () => {
      it('should find an element', async () => {
        const returnedFromService = Object.assign({}, elemDefault);
        service
          .find('123')
          .pipe(take(1))
          .subscribe(resp => (expectedResult = resp));

        const req = httpMock.expectOne({ method: 'GET' });
        req.flush(returnedFromService);
        expect(expectedResult).toMatchObject({ body: elemDefault });
      });

      it('should create a MemberServicesUser', async () => {
        const returnedFromService = Object.assign(
          {
            id: 'ID'
          },
          elemDefault
        );
        const expected = Object.assign({}, returnedFromService);
        service
          .create(new MemberServicesUser(null))
          .pipe(take(1))
          .subscribe(resp => (expectedResult = resp));
        const req = httpMock.expectOne({ method: 'POST' });
        req.flush(returnedFromService);
        expect(expectedResult).toMatchObject({ body: expected });
      });

      it('should update a MemberServicesUser', async () => {
        const returnedFromService = Object.assign(
          {
            user_id: 'BBBBBB',
            salesforceId: 'BBBBBB',
            parentSalesforceId: 'BBBBBB',
            disabled: true,
            mainContact: true,
            assertionServiceEnabled: true,
            oboClientId: 'BBBBBB'
          },
          elemDefault
        );

        const expected = Object.assign({}, returnedFromService);
        service
          .update(expected)
          .pipe(take(1))
          .subscribe(resp => (expectedResult = resp));
        const req = httpMock.expectOne({ method: 'PUT' });
        req.flush(returnedFromService);
        expect(expectedResult).toMatchObject({ body: expected });
      });

      it('should return a list of MemberServicesUser', async () => {
        const returnedFromService = Object.assign(
          {
            user_id: 'BBBBBB',
            salesforceId: 'BBBBBB',
            parentSalesforceId: 'BBBBBB',
            disabled: true,
            mainContact: true,
            assertionServiceEnabled: true,
            oboClientId: 'BBBBBB'
          },
          elemDefault
        );
        const expected = Object.assign({}, returnedFromService);
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

      it('should delete a MemberServicesUser', async () => {
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
