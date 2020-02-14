import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { GatewaySharedModule } from 'app/shared';
import { LANDING_PAGE_ROUTE, LandingPageComponent } from './';

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild([LANDING_PAGE_ROUTE])],
  declarations: [LandingPageComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayLandingPageModule {}
