import { NgModule } from '@angular/core';

import { GatewaySharedLibsModule, FindLanguageFromKeyPipe, JhiAlertComponent, JhiAlertErrorComponent, ConvertToCamelCasePipe } from './';

@NgModule({
  imports: [GatewaySharedLibsModule],
  declarations: [FindLanguageFromKeyPipe, ConvertToCamelCasePipe, JhiAlertComponent, JhiAlertErrorComponent],
  exports: [GatewaySharedLibsModule, FindLanguageFromKeyPipe, ConvertToCamelCasePipe, JhiAlertComponent, JhiAlertErrorComponent]
})
export class GatewaySharedCommonModule {}
