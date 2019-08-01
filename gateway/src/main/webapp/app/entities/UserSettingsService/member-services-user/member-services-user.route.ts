import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { MemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';
import { MemberServicesUserService } from './member-services-user.service';
import { MemberServicesUserComponent } from './member-services-user.component';
import { MemberServicesUserDetailComponent } from './member-services-user-detail.component';
import { MemberServicesUserUpdateComponent } from './member-services-user-update.component';
import { MemberServicesUserDeletePopupComponent } from './member-services-user-delete-dialog.component';
import { IMemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';

@Injectable({ providedIn: 'root' })
export class MemberServicesUserResolve implements Resolve<IMemberServicesUser> {
  constructor(private service: MemberServicesUserService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IMemberServicesUser> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<MemberServicesUser>) => response.ok),
        map((memberServicesUser: HttpResponse<MemberServicesUser>) => memberServicesUser.body)
      );
    }
    return of(new MemberServicesUser());
  }
}

export const memberServicesUserRoute: Routes = [
  {
    path: '',
    component: MemberServicesUserComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams
    },
    data: {
      authorities: ['ROLE_USER'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.userSettingsServiceMemberServicesUser.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    component: MemberServicesUserDetailComponent,
    resolve: {
      memberServicesUser: MemberServicesUserResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberServicesUser.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    component: MemberServicesUserUpdateComponent,
    resolve: {
      memberServicesUser: MemberServicesUserResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberServicesUser.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    component: MemberServicesUserUpdateComponent,
    resolve: {
      memberServicesUser: MemberServicesUserResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberServicesUser.home.title'
    },
    canActivate: [UserRouteAccessService]
  }
];

export const memberServicesUserPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: MemberServicesUserDeletePopupComponent,
    resolve: {
      memberServicesUser: MemberServicesUserResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberServicesUser.home.title'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  }
];
