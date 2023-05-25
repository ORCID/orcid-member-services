import { NgModule } from '@angular/core';

import {
  GatewaySharedLibsModule,
  FindLanguageFromKeyPipe,
  JhiAlertComponent,
  JhiAlertErrorComponent,
  ConvertToCamelCasePipe,
  ContactUpdateConfirmationAlert
} from './';
import { AddConsortiumMemberConfirmationComponent } from './alert/add-consortium-member/add-consortium-member-confirmation.component';

@NgModule({
  imports: [GatewaySharedLibsModule],
  declarations: [
    FindLanguageFromKeyPipe,
    ConvertToCamelCasePipe,
    JhiAlertComponent,
    JhiAlertErrorComponent,
    ContactUpdateConfirmationAlert,
    AddConsortiumMemberConfirmationComponent
  ],
  exports: [
    GatewaySharedLibsModule,
    FindLanguageFromKeyPipe,
    ConvertToCamelCasePipe,
    JhiAlertComponent,
    JhiAlertErrorComponent,
    ContactUpdateConfirmationAlert,
    AddConsortiumMemberConfirmationComponent
  ],
  entryComponents: [ContactUpdateConfirmationAlert, AddConsortiumMemberConfirmationComponent]
})
export class GatewaySharedCommonModule {}
