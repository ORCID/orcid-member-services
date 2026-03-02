import { Routes } from '@angular/router'
import { LoginComponent } from './login/login.component'
import { PasswordResetInitComponent } from './password/password-reset-init.component'
import { SettingsComponent } from './settings/settings.component'
import { AuthGuard } from './auth.guard'
import { PasswordComponent } from './password/password.component'
import { PasswordResetFinishComponent } from './password/password-reset-finish.component'
import { ActivationComponent } from './activation/activation.component'

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    data: {
      authorities: [],
      pageTitle: 'global.menu.account.login.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'reset/request',
    component: PasswordResetInitComponent,
    data: {
      authorities: [],
      pageTitle: 'global.menu.account.password.string',
    },
  },
  {
    path: 'reset/finish',
    component: PasswordResetFinishComponent,
    data: {
      authorities: [],
      pageTitle: 'global.menu.account.password.string',
    },
  },
  {
    path: 'settings',
    component: SettingsComponent,
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'global.menu.account.settings.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'password',
    component: PasswordComponent,
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'global.menu.account.password.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'activate',
    component: ActivationComponent,
    data: {
      authorities: [],
      pageTitle: 'activate.title.string',
    },
  },
]
