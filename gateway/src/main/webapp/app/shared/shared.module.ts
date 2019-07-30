import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { GatewaySharedCommonModule, JhiLoginModalComponent, HasAnyAuthorityDirective } from './';

@NgModule({
  imports: [GatewaySharedCommonModule],
  declarations: [JhiLoginModalComponent, HasAnyAuthorityDirective],
  entryComponents: [JhiLoginModalComponent],
  exports: [GatewaySharedCommonModule, JhiLoginModalComponent, HasAnyAuthorityDirective],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewaySharedModule {
  static forRoot() {
    return {
      ngModule: GatewaySharedModule
    };
  }
}
