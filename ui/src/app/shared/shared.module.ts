import { NgModule } from '@angular/core'
import { FindLanguageFromKeyPipe } from './pipe/find-language-from-key'

@NgModule({
  declarations: [FindLanguageFromKeyPipe],
  exports: [FindLanguageFromKeyPipe],
})
export class SharedModule {
  static forRoot() {
    return {
      ngModule: SharedModule,
    }
  }
}
