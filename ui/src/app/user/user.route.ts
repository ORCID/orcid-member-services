import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router'
import { UsersComponent } from './users.component'
import { AuthGuard } from '../account/auth.guard'
import { UserDetailComponent } from './user-detail.component'
import { EMPTY, Observable, filter, of, take } from 'rxjs'
import { User } from './model/user.model'
import { UserService } from './service/user.service'
import { Injectable, inject } from '@angular/core'
import { UserUpdateComponent } from './user-update.component'

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
      user: UserResolver
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'users/:id/edit',
    component: UserUpdateComponent,
    resolve: {
      user: UserResolver
    },
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string'
    },
    canActivate: [AuthGuard]
  }
]

