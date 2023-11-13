import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { LoginComponent } from './login/login.component'
import { RouterModule } from '@angular/router'
import { ReactiveFormsModule } from '@angular/forms'
import { routes } from './account.route';
import { PasswordResetInitComponent } from './password/password-reset-init.component'

@NgModule({
  declarations: [LoginComponent, PasswordResetInitComponent],
  imports: [CommonModule, ReactiveFormsModule, RouterModule.forChild(routes)],
})
export class AccountModule {}
