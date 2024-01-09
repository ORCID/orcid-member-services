import { NgModule } from '@angular/core'
import { FindLanguageFromKeyPipe } from './pipe/find-language-from-key'
import { ErrorAlertComponent } from './error/error-alert.component'
import { CommonModule } from '@angular/common'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'

@NgModule({
  imports: [CommonModule, NgbModule],
  declarations: [FindLanguageFromKeyPipe, ErrorAlertComponent],
  exports: [FindLanguageFromKeyPipe, ErrorAlertComponent],
})
export class SharedModule {
  static forRoot() {
    return {
      ngModule: SharedModule,
    }
  }
}
