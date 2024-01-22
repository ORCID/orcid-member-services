import { NgModule } from '@angular/core'
import { FindLanguageFromKeyPipe } from './pipe/find-language-from-key'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { CommonModule } from '@angular/common'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { HasAnyAuthorityDirective } from './directive/has-any-authority.directive'
import { AlertComponent } from './alert/alert.component'
import { LocalizePipe } from './pipe/localize'

@NgModule({
  imports: [CommonModule, NgbModule, FontAwesomeModule],
  declarations: [FindLanguageFromKeyPipe, LocalizePipe, ErrorAlertComponent, HasAnyAuthorityDirective, AlertComponent],
  exports: [
    FindLanguageFromKeyPipe,
    LocalizePipe,
    ErrorAlertComponent,
    AlertComponent,
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
