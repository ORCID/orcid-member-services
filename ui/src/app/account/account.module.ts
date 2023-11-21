import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { LoginComponent } from './login/login.component'
import { RouterModule } from '@angular/router'
import { ReactiveFormsModule } from '@angular/forms'
import { routes } from './account.route'
import { PasswordResetInitComponent } from './password/password-reset-init.component'
import { SettingsComponent } from './settings/settings.component'
import { SharedModule } from '../shared/shared.module'

@NgModule({
  declarations: [LoginComponent, PasswordResetInitComponent, SettingsComponent],
  imports: [SharedModule, CommonModule, ReactiveFormsModule, RouterModule.forChild(routes)],
})
export class AccountModule {}
