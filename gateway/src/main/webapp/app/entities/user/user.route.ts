import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { MSUser } from 'app/shared/model/user.model';
import { MSUserService } from './user.service';
import { MSUserComponent } from './user.component';
import { MSUserDetailComponent } from './user-detail.component';
import { MSUserUpdateComponent } from './user-update.component';
import { MSUserDeletePopupComponent } from './user-delete-dialog.component';
import { MSUserImportPopupComponent } from './user-import-dialog.component';
import { IMSUser } from 'app/shared/model/user.model';

@Injectable({ providedIn: 'root' })
export class MSUserResolve implements Resolve<IMSUser> {
  constructor(private service: MSUserService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IMSUser> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<MSUser>) => response.ok),
        map((msUser: HttpResponse<MSUser>) => msUser.body)
      );
    }
    return of(new MSUser());
  }
}

export const msUserRoute: Routes = [
  {
    path: '',
    component: MSUserComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    component: MSUserDetailComponent,
    resolve: {
      msUser: MSUserResolve
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    component: MSUserUpdateComponent,
    resolve: {
      msUser: MSUserResolve
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    component: MSUserUpdateComponent,
    resolve: {
      msUser: MSUserResolve
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  }
];

export const msUserPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: MSUserDeletePopupComponent,
    resolve: {
      msUser: MSUserResolve
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  },
  {
    path: 'import',
    component: MSUserImportPopupComponent,
    resolve: {
      msUser: MSUserResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  }
];
