import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router'
import { AuthGuard } from '../account/auth.guard'
import { EMPTY, Observable, filter, of, take } from 'rxjs'
import { Injectable, inject } from '@angular/core'
import { AffiliationService } from './service/affiliations.service'
import { Affiliation } from './model/affiliation.model'
import { AffiliationsComponent } from './affiliations.component'
import { HttpResponse } from '@angular/common/http'

export const AffiliationResolver: ResolveFn<Affiliation | null> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  affiliationService: AffiliationService = inject(AffiliationService)
): Observable<Affiliation | null> => {
  if (route.paramMap.get('id')) {
    return affiliationService.find(route.paramMap.get('id')!)
  } else {
    return of(null)
  }
}

export const assertionRoute: Routes = [
  {
    path: '',
    component: AssertionComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      defaultSort: 'email,asc',
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: AssertionDetailComponent,
    resolve: {
      assertion: AssertionResolve,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: AssertionUpdateComponent,
    resolve: {
      assertion: AssertionResolve,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: AssertionUpdateComponent,
    resolve: {
      assertion: AssertionResolve,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [UserRouteAccessService],
  },
]

export const assertionPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: AssertionDeletePopupComponent,
    resolve: {
      assertion: AssertionResolve,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup',
  },
  {
    path: 'import',
    component: AssertionImportPopupComponent,
    resolve: {
      assertion: AssertionResolve,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup',
  },
  {
    path: 'notifications',
    component: SendNotificationsPopupComponent,
    resolve: {
      assertion: AssertionResolve,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup',
  },
]
