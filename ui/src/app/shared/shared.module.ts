import { CommonModule } from '@angular/common';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { GatewaySharedCommonModule, /*JhiLoginModalComponent,*/ HasAnyAuthorityDirective, LoginComponent } from './';

@NgModule({
  imports: [GatewaySharedCommonModule],
  declarations: [/*JhiLoginModalComponent,*/ HasAnyAuthorityDirective, LoginComponent],
 /* entryComponents: [JhiLoginModalComponent],*/
  exports: [GatewaySharedCommonModule, /*JhiLoginModalComponent,*/ HasAnyAuthorityDirective],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewaySharedModule {
  static forRoot() {
    return {
      ngModule: GatewaySharedModule
    };
  }
}
