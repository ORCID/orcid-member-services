import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { GatewaySharedModule } from 'app/shared';
import {
  MemberServicesUserComponent,
  MemberServicesUserDetailComponent,
  MemberServicesUserUpdateComponent,
  MemberServicesUserDeletePopupComponent,
  MemberServicesUserDeleteDialogComponent,
  memberServicesUserRoute,
  memberServicesUserPopupRoute
} from './';

const ENTITY_STATES = [...memberServicesUserRoute, ...memberServicesUserPopupRoute];

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild(ENTITY_STATES)],
  declarations: [
    MemberServicesUserComponent,
    MemberServicesUserDetailComponent,
    MemberServicesUserUpdateComponent,
    MemberServicesUserDeleteDialogComponent,
    MemberServicesUserDeletePopupComponent
  ],
  entryComponents: [
    MemberServicesUserComponent,
    MemberServicesUserUpdateComponent,
    MemberServicesUserDeleteDialogComponent,
    MemberServicesUserDeletePopupComponent
  ],
  providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class UserSettingsServiceMemberServicesUserModule {
  constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
    this.languageHelper.language.subscribe((languageKey: string) => {
      if (languageKey !== undefined) {
        this.languageService.changeLanguage(languageKey);
      }
    });
  }
}
