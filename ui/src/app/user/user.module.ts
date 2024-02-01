import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { UsersComponent } from './users.component'
import { RouterModule } from '@angular/router'
import { routes } from './user.route'
import { SharedModule } from '../shared/shared.module'
import { BrowserModule } from '@angular/platform-browser'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { HasAnyAuthorityDirective } from '../shared/directive/has-any-authority.directive';
import { UserUpdateComponent } from './user-update.component'
import { UserDetailComponent } from './user-detail.component'

@NgModule({
  declarations: [UsersComponent, UserUpdateComponent, UserDetailComponent],
  imports: [CommonModule, SharedModule, RouterModule.forChild(routes), FontAwesomeModule, FormsModule, ReactiveFormsModule],
})
export class UserModule {}
