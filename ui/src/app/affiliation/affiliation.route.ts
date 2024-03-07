import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router'
import { AuthGuard } from '../account/auth.guard'
import { Observable, filter, of, take } from 'rxjs'
import { inject } from '@angular/core'
import { AffiliationService } from './service/affiliations.service'
import { Affiliation } from './model/affiliation.model'
import { AffiliationsComponent } from './affiliations.component'
import { AffiliationDetailComponent } from './affiliation-detail.component'
import { AffiliationImportPopupComponent } from './affiliation-import-dialog.component'

export const AffiliationResolver: ResolveFn<Affiliation | null> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  affiliationService: AffiliationService = inject(AffiliationService)
): Observable<Affiliation | null> => {
  if (route.paramMap.get('id')) {
    return affiliationService.find(route.paramMap.get('id')!).pipe(
      filter<Affiliation>((affiliation: Affiliation) => !!affiliation),
      take(1)
    )
  } else {
    return of(null)
  }
}

export const affiliationRoutes: Routes = [
  {
    path: 'affiliations',
    component: AffiliationsComponent,
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      defaultSort: 'email,asc',
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [AuthGuard],
    children: [
      {
        path: 'import',
        component: AffiliationImportPopupComponent,
        resolve: {
          affiliation: AffiliationResolver,
        },
        data: {
          authorities: ['ASSERTION_SERVICE_ENABLED'],
          pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
        },
        canActivate: [AuthGuard],
        outlet: 'popup',
      },
    ],
  },
  {
    path: 'affiliations/:id/view',
    component: AffiliationDetailComponent,
    resolve: {
      affiliation: AffiliationResolver,
    },
    data: {
      authorities: ['ASSERTION_SERVICE_ENABLED'],
      pageTitle: 'gatewayApp.assertionServiceAssertion.home.title.string',
    },
    canActivate: [AuthGuard],
  },
]
