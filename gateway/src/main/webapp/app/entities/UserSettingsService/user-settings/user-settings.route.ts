import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { UserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';
import { UserSettingsService } from './user-settings.service';
import { UserSettingsComponent } from './user-settings.component';
import { UserSettingsDetailComponent } from './user-settings-detail.component';
import { UserSettingsUpdateComponent } from './user-settings-update.component';
import { UserSettingsDeletePopupComponent } from './user-settings-delete-dialog.component';
import { UserSettingsImportPopupComponent } from './user-settings-import-dialog.component';
import { IUserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';

@Injectable({ providedIn: 'root' })
export class UserSettingsResolve implements Resolve<IUserSettings> {
  constructor(private service: UserSettingsService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IUserSettings> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<UserSettings>) => response.ok),
        map((userSettings: HttpResponse<UserSettings>) => userSettings.body)
      );
    }
    return of(new UserSettings());
  }
}

export const userSettingsRoute: Routes = [
  {
    path: '',
    component: UserSettingsComponent,
    resolve: {
      pagingParams: JhiResolvePagingParams
    },
    data: {
      authorities: ['ROLE_USER'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.userSettingsServiceUserSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    component: UserSettingsDetailComponent,
    resolve: {
      userSettings: UserSettingsResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceUserSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    component: UserSettingsUpdateComponent,
    resolve: {
      userSettings: UserSettingsResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceUserSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    component: UserSettingsUpdateComponent,
    resolve: {
      userSettings: UserSettingsResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceUserSettings.home.title'
    },
    canActivate: [UserRouteAccessService]
  }
];

export const userSettingsPopupRoute: Routes = [
  {
    path: ':id/delete',
    component: UserSettingsDeletePopupComponent,
    resolve: {
      userSettings: UserSettingsResolve
    },
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'gatewayApp.userSettingsServiceUserSettings.home.title'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  },
  {
    path: 'import',
    component: UserSettingsImportPopupComponent,
    resolve: {
      userSettings: UserSettingsResolve
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.userSettingsServiceUserSettings.home.title'
    },
    canActivate: [UserRouteAccessService],
    outlet: 'popup'
  }
];
