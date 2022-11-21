import { CommonModule } from '@angular/common';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HomeComponent, HomeModule } from 'app/home';
import { GatewaySharedCommonModule, /*JhiLoginModalComponent,*/ HasAnyAuthorityDirective } from './';

@NgModule({
  imports: [GatewaySharedCommonModule, BrowserModule, CommonModule],
  declarations: [/*JhiLoginModalComponent,*/ HasAnyAuthorityDirective],
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
