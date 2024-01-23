import { Routes } from '@angular/router'
import { UsersComponent } from './users.component'
import { AuthGuard } from '../account/auth.guard'

export const routes: Routes = [
  {
    path: 'users',
    component: UsersComponent,
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER', 'ROLE_CONSORTIUM_LEAD'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.string',
    },
    canActivate: [AuthGuard],
  },
]
