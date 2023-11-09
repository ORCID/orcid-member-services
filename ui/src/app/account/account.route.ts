import { Routes } from '@angular/router'
import { LoginComponent } from './login/login.component'
import { PasswordResetInitComponent } from './password/password-reset-init.component'

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
]
