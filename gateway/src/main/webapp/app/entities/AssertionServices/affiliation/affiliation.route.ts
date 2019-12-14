import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { Affiliation } from 'app/shared/model/AssertionServices/affiliation.model';
import { AffiliationService } from './affiliation.service';
import { AffiliationComponent } from './affiliation.component';
import { AffiliationDetailComponent } from './affiliation-detail.component';
import { AffiliationUpdateComponent } from './affiliation-update.component';
import { AffiliationDeletePopupComponent } from './affiliation-delete-dialog.component';
import { IAffiliation } from 'app/shared/model/AssertionServices/affiliation.model';

@Injectable({ providedIn: 'root' })
export class AffiliationResolve implements Resolve<IAffiliation> {
  constructor(private service: AffiliationService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IAffiliation> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<Affiliation>) => response.ok),
        map((affiliation: HttpResponse<Affiliation>) => affiliation.body)
      );
    }
    return of(new Affiliation());
  }
}

export const affiliationRoute: Routes = [
  {
    path: '',
    component: AffiliationComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams
    },
    data: {
      authorities: ['ROLE_USER'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.assertionServicesAffiliation.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    component: AffiliationDetailComponent,
    resolve: {
      affiliation: AffiliationResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.assertionServicesAffiliation.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    component: AffiliationUpdateComponent,
    resolve: {
      affiliation: AffiliationResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.assertionServicesAffiliation.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    component: AffiliationUpdateComponent,
    resolve: {
      affiliation: AffiliationResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.assertionServicesAffiliation.home.title'
    },
    canActivate: [UserRouteAccessService]
  }
];

export const affiliationPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: AffiliationDeletePopupComponent,
    resolve: {
      affiliation: AffiliationResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.assertionServicesAffiliation.home.title'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  }
];
