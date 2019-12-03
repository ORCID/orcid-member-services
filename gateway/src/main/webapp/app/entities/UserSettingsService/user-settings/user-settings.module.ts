import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { GatewaySharedModule } from 'app/shared';
import {
  UserSettingsComponent,
  UserSettingsDetailComponent,
  UserSettingsUpdateComponent,
  UserSettingsDeletePopupComponent,
  UserSettingsDeleteDialogComponent,
  UserSettingsImportPopupComponent,
  UserSettingsImportDialogComponent,
  userSettingsRoute,
  userSettingsPopupRoute
} from './';

const ENTITY_STATES = [...userSettingsRoute, ...userSettingsPopupRoute];

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild(ENTITY_STATES)],
  declarations: [
    UserSettingsComponent,
    UserSettingsDetailComponent,
    UserSettingsUpdateComponent,
    UserSettingsDeleteDialogComponent,
    UserSettingsDeletePopupComponent,
    UserSettingsImportDialogComponent,
    UserSettingsImportPopupComponent
  ],
  entryComponents: [
    UserSettingsComponent,
    UserSettingsUpdateComponent,
    UserSettingsDeleteDialogComponent,
    UserSettingsDeletePopupComponent,
    UserSettingsImportDialogComponent,
    UserSettingsImportPopupComponent
  ],
  providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class UserSettingsServiceUserSettingsModule {
  constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
    this.languageHelper.language.subscribe((languageKey: string) => {
      if (languageKey !== undefined) {
        this.languageService.changeLanguage(languageKey);
      }
    });
  }
}
