import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { MemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';
import { MemberSettingsService } from './member-settings.service';
import { MemberSettingsComponent } from './member-settings.component';
import { MemberSettingsDetailComponent } from './member-settings-detail.component';
import { MemberSettingsUpdateComponent } from './member-settings-update.component';
import { MemberSettingsImportPopupComponent } from './member-settings-import-dialog.component';
import { MemberSettingsDeletePopupComponent } from './member-settings-delete-dialog.component';
import { IMemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';

@Injectable({ providedIn: 'root' })
export class MemberSettingsResolve implements Resolve<IMemberSettings> {
  constructor(private service: MemberSettingsService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IMemberSettings> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<MemberSettings>) => response.ok),
        map((memberSettings: HttpResponse<MemberSettings>) => memberSettings.body)
      );
    }
    return of(new MemberSettings());
  }
}

export const memberSettingsRoute: Routes = [
  {
    path: '',
    component: MemberSettingsComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.userSettingsServiceMemberSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    component: MemberSettingsDetailComponent,
    resolve: {
      memberSettings: MemberSettingsResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    component: MemberSettingsUpdateComponent,
    resolve: {
      memberSettings: MemberSettingsResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    component: MemberSettingsUpdateComponent,
    resolve: {
      memberSettings: MemberSettingsResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  }
];

export const memberSettingsPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: MemberSettingsDeletePopupComponent,
    resolve: {
      memberSettings: MemberSettingsResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.userSettingsServiceMemberSettings.home.title'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  },
  {
      path: 'import',
      component: MemberSettingsImportPopupComponent,
      resolve: {
        memberSettings: MemberSettingsResolve
      },
      data: {
        authorities: ['ROLE_ADMIN'],
        pageTitle: 'gatewayApp.memberSettingsServiceUserSettings.home.title'
      },
      canActivate: [UserRouteAccessService],
      outlet: 'popup'
    }
];
