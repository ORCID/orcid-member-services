import { NgModule } from '@angular/core'
import { FindLanguageFromKeyPipe } from './pipe/find-language-from-key';
import { ErrorAlertComponent } from './error/error-alert.component'

@NgModule({
  declarations: [FindLanguageFromKeyPipe, ErrorAlertComponent],
  exports: [FindLanguageFromKeyPipe],
})
export class SharedModule {
  static forRoot() {
    return {
      ngModule: SharedModule,
    }
  }
}
