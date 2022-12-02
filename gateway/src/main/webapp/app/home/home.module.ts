import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { GatewaySharedModule } from 'app/shared';
import { HOME_ROUTE, HomeComponent } from './';
import { GenericLandingComponent } from './generic-landing/generic-landing.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild([HOME_ROUTE])],
  declarations: [HomeComponent, GenericLandingComponent, MemberInfoLandingComponent, MemberInfoEditComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayHomeModule {}
