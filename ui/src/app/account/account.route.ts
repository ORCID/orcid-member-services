import { Routes } from '@angular/router'
import { LoginComponent } from './login/login.component'
import { PasswordResetInitComponent } from './password/password-reset-init.component'
import { SettingsComponent } from './settings/settings.component'
import { AuthGuard } from './auth.guard'

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
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
    path: 'settings',
    component: SettingsComponent,
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'global.menu.account.settings.string',
    },
    canActivate: [AuthGuard],
  },
]
