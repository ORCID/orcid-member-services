import { NgModule } from '@angular/core';

import {
  GatewaySharedLibsModule,
  FindLanguageFromKeyPipe,
  JhiAlertComponent,
  JhiAlertErrorComponent,
  ConvertToCamelCasePipe,
  ContactUpdateConfirmationAlert
} from './';

@NgModule({
  imports: [GatewaySharedLibsModule],
  declarations: [
    FindLanguageFromKeyPipe,
    ConvertToCamelCasePipe,
    JhiAlertComponent,
    JhiAlertErrorComponent,
    ContactUpdateConfirmationAlert
  ],
  exports: [
    GatewaySharedLibsModule,
    FindLanguageFromKeyPipe,
    ConvertToCamelCasePipe,
    JhiAlertComponent,
    JhiAlertErrorComponent,
    ContactUpdateConfirmationAlert
  ],
  entryComponents: [ContactUpdateConfirmationAlert]
})
export class GatewaySharedCommonModule {}
