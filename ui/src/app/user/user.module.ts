import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { UsersComponent } from './users.component'
import { RouterModule } from '@angular/router'
import { routes } from './user.route'
import { SharedModule } from '../shared/shared.module'
import { BrowserModule } from '@angular/platform-browser'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { FormsModule } from '@angular/forms'
import { HasAnyAuthorityDirective } from '../shared/directive/has-any-authority.directive'

@NgModule({
  declarations: [UsersComponent],
  imports: [CommonModule, SharedModule, RouterModule.forChild(routes), FontAwesomeModule, FormsModule],
})
export class UserModule {}
