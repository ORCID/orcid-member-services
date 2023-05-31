import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { GatewaySharedModule } from 'app/shared';
import {
  HOME_ROUTE,
  HomeComponent,
  AddConsortiumMemberComponent,
  RemoveConsortiumMemberComponent,
  GenericLandingComponent,
  MemberInfoLandingComponent,
  MemberInfoEditComponent,
  ContactUpdateComponent
} from './';
import { TextFieldModule } from '@angular/cdk/text-field';
import { QuillModule } from 'ngx-quill';

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild([HOME_ROUTE]), TextFieldModule, QuillModule.forRoot()],
  declarations: [
    HomeComponent,
    GenericLandingComponent,
    MemberInfoLandingComponent,
    MemberInfoEditComponent,
    ContactUpdateComponent,
    AddConsortiumMemberComponent,
    RemoveConsortiumMemberComponent
  ],

  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayHomeModule {}
