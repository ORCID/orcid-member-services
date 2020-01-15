import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { GatewaySharedModule } from 'app/shared';
import {
  MemberSettingsComponent,
  MemberSettingsDetailComponent,
  MemberSettingsUpdateComponent,
  MemberSettingsDeletePopupComponent,
  MemberSettingsDeleteDialogComponent,
  MemberSettingsImportPopupComponent,
  MemberSettingsImportDialogComponent,
  memberSettingsRoute,
  memberSettingsPopupRoute
} from './';

const ENTITY_STATES = [...memberSettingsRoute, ...memberSettingsPopupRoute];

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild(ENTITY_STATES)],
  declarations: [
    MemberSettingsComponent,
    MemberSettingsDetailComponent,
    MemberSettingsUpdateComponent,
    MemberSettingsDeleteDialogComponent,
    MemberSettingsDeletePopupComponent,
    MemberSettingsImportDialogComponent,
    MemberSettingsImportPopupComponent
  ],
  entryComponents: [
    MemberSettingsComponent,
    MemberSettingsUpdateComponent,
    MemberSettingsDeleteDialogComponent,
    MemberSettingsDeletePopupComponent,
    MemberSettingsImportDialogComponent,
    MemberSettingsImportPopupComponent
  ],
  providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class UserSettingsServiceMemberSettingsModule {
  constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
    this.languageHelper.language.subscribe((languageKey: string) => {
      if (languageKey !== undefined) {
        this.languageService.changeLanguage(languageKey);
      }
    });
  }
}
