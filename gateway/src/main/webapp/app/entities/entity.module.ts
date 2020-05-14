import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'ms-user',
        loadChildren: './MSUserService/ms-users/ms-user.module#MSUserServiceMSUserModule'
      },
      {
        path: 'ms-member',
        loadChildren: './MSUserService/ms-members/ms-member.module#MSUserServiceMSMemberModule'
      },
      {
        path: 'assertions',
        loadChildren: './AssertionService/assertion/assertion.module#AssertionServiceAssertionModule'
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
