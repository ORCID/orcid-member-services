import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'user-settings',
        loadChildren: './UserSettingsService/user-settings/user-settings.module#UserSettingsServiceUserSettingsModule'
      },
      {
        path: 'member-settings',
        loadChildren: './UserSettingsService/member-settings/member-settings.module#UserSettingsServiceMemberSettingsModule'
      }
      /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
    ])
  ],
  declarations: [],
  entryComponents: [],
  providers: [],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayEntityModule {}
