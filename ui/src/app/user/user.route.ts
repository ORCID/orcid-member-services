import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router'
import { UsersComponent } from './users.component'
import { AuthGuard } from '../account/auth.guard'
import { UserDetailComponent } from './user-detail.component'
import { Observable, filter, of, take } from 'rxjs'
import { User } from './model/user.model'
import { UserService } from './service/user.service'
import { inject } from '@angular/core'
import { UserUpdateComponent } from './user-update.component'
import { UserImportPopupComponent } from './user-import-dialog.component'
import { UserDeletePopupComponent } from './user-delete.component'

export const UserResolver: ResolveFn<User | null> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  userService: UserService = inject(UserService)
): Observable<User | null> => {
  if (route.paramMap.get('id')) {
    return userService.find(route.paramMap.get('id')!).pipe(
      filter<User>((user: User) => !!user),
      take(1)
    )
  } else {
    return of(null)
  }
}

export const routes: Routes = [
  {
    path: 'users',
    component: UsersComponent,
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
      ascending: true,
    },
    canActivate: [AuthGuard],
    children: [
      {
        path: ':id/delete',
        component: UserDeletePopupComponent,
        resolve: {
          user: UserResolver,
        },
        data: {
          authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
          pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
        },
        canActivate: [AuthGuard],
        outlet: 'popup',
      },
      {
        path: 'import',
        component: UserImportPopupComponent,
        resolve: {
          user: UserResolver,
        },
        data: {
          authorities: ['ROLE_ADMIN'],
          pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
        },
        canActivate: [AuthGuard],
        outlet: 'popup',
      },
    ],
  },
  {
    path: 'users/:id/view',
    component: UserDetailComponent,
    resolve: {
      user: UserResolver,
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'users/new',
    component: UserUpdateComponent,
    resolve: {
      user: UserResolver,
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'users/:id/edit',
    component: UserUpdateComponent,
    resolve: {
      user: UserResolver,
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
    },
    canActivate: [AuthGuard],
  },
]
