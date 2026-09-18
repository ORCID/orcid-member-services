import { ActivatedRouteSnapshot, ResolveFn, Routes } from '@angular/router'
import { inject } from '@angular/core'
import { Observable, take, throwError } from 'rxjs'
import { AuthGuard } from '../account/auth.guard'
import { ApiCredentialsComponent } from './api-credentials.component'
import { ProductionCredentialsApplyComponent } from './production-credentials-apply.component'
import { ApiCredentialsService } from './service/api-credentials.service'
import { Client } from './model/client'

export const ApiCredentialResolver: ResolveFn<Client> = (
  route: ActivatedRouteSnapshot,
  _state,
  service: ApiCredentialsService = inject(ApiCredentialsService)
): Observable<Client> => {
  const clientId = route.paramMap.get('clientId')
  return clientId ? service.get(clientId).pipe(take(1)) : throwError(() => new Error('Client ID is required'))
}

export const routes: Routes = [
  {
    path: '',
    component: ApiCredentialsComponent,
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER'],
      pageTitle: 'gatewayApp.msUserServiceMSApiCredentials.home.title.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'new',
    component: ProductionCredentialsApplyComponent,
    data: {
      authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER'],
      pageTitle: 'gatewayApp.msUserServiceMSApiCredentials.home.title.string',
    },
    canActivate: [AuthGuard],
  },
]
