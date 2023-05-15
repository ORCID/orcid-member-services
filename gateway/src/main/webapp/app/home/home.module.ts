import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { GatewaySharedModule } from 'app/shared';
import { HOME_ROUTE, HomeComponent } from './';
import { GenericLandingComponent } from './';
import { MemberInfoLandingComponent } from './';
import { MemberInfoEditComponent } from './';
import { TextFieldModule } from '@angular/cdk/text-field';
import { QuillModule } from 'ngx-quill';
import { ContactUpdateComponent } from './';

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild([HOME_ROUTE]), TextFieldModule, QuillModule.forRoot()],
  declarations: [HomeComponent, GenericLandingComponent, MemberInfoLandingComponent, MemberInfoEditComponent, ContactUpdateComponent],

  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayHomeModule {}
