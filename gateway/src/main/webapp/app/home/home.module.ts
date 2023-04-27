import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { GatewaySharedModule } from 'app/shared';
import { HOME_ROUTE, HomeComponent } from './';
import { GenericLandingComponent } from './generic-landing/generic-landing.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { TextFieldModule } from '@angular/cdk/text-field';
import { QuillModule } from 'ngx-quill';
import { ContactAddComponent } from './member-info-landing/contact-add/contact-add.component';
import { ContactEditComponent } from './member-info-landing/contact-edit/contact-edit.component';

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild([HOME_ROUTE]), TextFieldModule, QuillModule.forRoot()],
  declarations: [
    HomeComponent,
    GenericLandingComponent,
    MemberInfoLandingComponent,
    MemberInfoEditComponent,
    ContactAddComponent,
    ContactEditComponent
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayHomeModule {}
