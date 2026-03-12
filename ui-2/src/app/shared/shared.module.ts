import { NgModule } from '@angular/core'
import { FindLanguageFromKeyPipe } from './pipe/find-language-from-key'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { CommonModule } from '@angular/common'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { HasAnyAuthorityDirective } from './directive/has-any-authority.directive'
import { AlertComponent } from './alert/alert-toast.component'
import { LocalizePipe } from './pipe/localize'
import { AddConsortiumMemberAlertComponent } from './alert/consortium-member/add-consortium-member-alert.component'
import { RemoveConsortiumMemberAlertComponent } from './alert/consortium-member/remove-consortium-member-alert.component'
import { ContactUpdateAlertComponent } from './alert/contact-update/contact-update-alert.component'

@NgModule({
  imports: [NgbModule, FontAwesomeModule, CommonModule],
  declarations: [
    FindLanguageFromKeyPipe,
    LocalizePipe,
    ErrorAlertComponent,
    HasAnyAuthorityDirective,
    AlertComponent,
    AddConsortiumMemberAlertComponent,
    RemoveConsortiumMemberAlertComponent,
    ContactUpdateAlertComponent,
  ],
  exports: [
    FindLanguageFromKeyPipe,
    LocalizePipe,
    ErrorAlertComponent,
    AlertComponent,
    AddConsortiumMemberAlertComponent,
    RemoveConsortiumMemberAlertComponent,
    ContactUpdateAlertComponent,
    NgbModule,
    FontAwesomeModule,
    HasAnyAuthorityDirective,
  ],
})
export class SharedModule {
  static forRoot() {
    return {
      ngModule: SharedModule,
    }
  }
}
