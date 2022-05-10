import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { Assertion } from 'app/shared/model/assertion.model';
import { AssertionService } from './assertion.service';
import { AssertionComponent } from './assertion.component';
import { AssertionDetailComponent } from './assertion-detail.component';
import { AssertionUpdateComponent } from './assertion-update.component';
import { AssertionDeletePopupComponent } from './assertion-delete-dialog.component';
import { AssertionImportPopupComponent } from './assertion-import-dialog.component';
import { SendNotificationsPopupComponent } from './send-notifications-dialog.component';
import { IAssertion } from 'app/shared/model/assertion.model';

@Injectable({ providedIn: 'root' })
export class AssertionResolve implements Resolve<IAssertion> {
  constructor(private service: AssertionService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IAssertion> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<Assertion>) => response.ok),
        map((assertion: HttpResponse<Assertion>) => assertion.body)
      );
    }
    return of(new Assertion());
  }
}

export const assertionRoute: Routes = [
  {
    path: '',
    component: AssertionComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      defaultSort: 'email,asc',
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    component: AssertionDetailComponent,
    resolve: {
      assertion: AssertionResolve
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    component: AssertionUpdateComponent,
    resolve: {
      assertion: AssertionResolve
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    component: AssertionUpdateComponent,
    resolve: {
      assertion: AssertionResolve
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  }
];

export const assertionPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: AssertionDeletePopupComponent,
    resolve: {
      assertion: AssertionResolve
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  },
  {
    path: 'import',
    component: AssertionImportPopupComponent,
    resolve: {
      assertion: AssertionResolve
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  },
  {
    path: 'notifications',
    component: SendNotificationsPopupComponent,
    resolve: {
      assertion: AssertionResolve
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  }
];
