import { NgModule } from '@angular/core';

import {
  GatewaySharedLibsModule,
  FindLanguageFromKeyPipe,
  JhiAlertComponent,
  JhiAlertErrorComponent,
  ConvertToCamelCasePipe,
  ContactUpdateConfirmationAlert
} from './';
import { RemoveConsortiumMemberConfirmationComponent } from './alert/consortium-members/remove-consortium-member-confirmation.component';
import { AddConsortiumMemberConfirmationComponent } from './alert/consortium-members/add-consortium-member-confirmation.component';

@NgModule({
  imports: [GatewaySharedLibsModule],
  declarations: [
    FindLanguageFromKeyPipe,
    ConvertToCamelCasePipe,
    JhiAlertComponent,
    JhiAlertErrorComponent,
    ContactUpdateConfirmationAlert,
    AddConsortiumMemberConfirmationComponent,
    RemoveConsortiumMemberConfirmationComponent
  ],
  exports: [
    GatewaySharedLibsModule,
    FindLanguageFromKeyPipe,
    ConvertToCamelCasePipe,
    JhiAlertComponent,
    JhiAlertErrorComponent,
    ContactUpdateConfirmationAlert,
    AddConsortiumMemberConfirmationComponent,
    RemoveConsortiumMemberConfirmationComponent
  ],
  entryComponents: [ContactUpdateConfirmationAlert, AddConsortiumMemberConfirmationComponent, RemoveConsortiumMemberConfirmationComponent]
})
export class GatewaySharedCommonModule {}
