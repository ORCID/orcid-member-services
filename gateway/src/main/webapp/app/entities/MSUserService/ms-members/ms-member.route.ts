import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { MSMember } from 'app/shared/model/MSUserService/ms-member.model';
import { MSMemberService } from './ms-member.service';
import { MSMemberComponent } from './ms-member.component';
import { MSMemberDetailComponent } from './ms-member-detail.component';
import { MSMemberUpdateComponent } from './ms-member-update.component';
import { MSMemberImportPopupComponent } from './ms-member-import-dialog.component';
import { MSMemberDeletePopupComponent } from './ms-member-delete-dialog.component';
import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';

@Injectable({ providedIn: 'root' })
export class MSMemberResolve implements Resolve<IMSMember> {
  constructor(private service: MSMemberService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IMSMember> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<MSMember>) => response.ok),
        map((msMember: HttpResponse<MSMember>) => msMember.body)
      );
    }
    return of(new MSMember());
  }
}

export const msMemberRoute: Routes = [
  {
    path: '',
    component: MSMemberComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    component: MSMemberDetailComponent,
    resolve: {
      msMember: MSMemberResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    component: MSMemberUpdateComponent,
    resolve: {
      msMember: MSMemberResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    component: MSMemberUpdateComponent,
    resolve: {
      msMember: MSMemberResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string'
    },
    canActivate: [UserRouteAccessService]
  }
];

export const msMemberPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: MSMemberDeletePopupComponent,
    resolve: {
      msMember: MSMemberResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  },
  {
    path: 'import',
    component: MSMemberImportPopupComponent,
    resolve: {
      msMember: MSMemberResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  }
];
