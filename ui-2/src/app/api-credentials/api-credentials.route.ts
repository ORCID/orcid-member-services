import { Routes } from '@angular/router'
import { AuthGuard } from '../account/auth.guard'
import { ApiCredentialsComponent } from './api-credentials.component'

export const routes: Routes = [
  {
    path: '',
    component: ApiCredentialsComponent,
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER'],
      pageTitle: 'gatewayApp.msUserServiceMSApiCredentials.home.title.string',
    },
    canActivate: [AuthGuard],
  }
]
