import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { GatewaySharedModule } from 'app/shared';
import {
  MSMemberComponent,
  MSMemberDetailComponent,
  MSMemberUpdateComponent,
  MSMemberDeletePopupComponent,
  MSMemberDeleteDialogComponent,
  MSMemberImportPopupComponent,
  MSMemberImportDialogComponent,
  msMemberRoute,
  msMemberPopupRoute
} from './';

const ENTITY_STATES = [...msMemberRoute, ...msMemberPopupRoute];

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild(ENTITY_STATES)],
  declarations: [
    MSMemberComponent,
    MSMemberDetailComponent,
    MSMemberUpdateComponent,
    MSMemberDeleteDialogComponent,
    MSMemberDeletePopupComponent,
    MSMemberImportDialogComponent,
    MSMemberImportPopupComponent
  ],
  entryComponents: [
    MSMemberComponent,
    MSMemberUpdateComponent,
    MSMemberDeleteDialogComponent,
    MSMemberDeletePopupComponent,
    MSMemberImportDialogComponent,
    MSMemberImportPopupComponent
  ],
  providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class MSUserServiceMSMemberModule {
  constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
    this.languageHelper.language.subscribe((languageKey: string) => {
      if (languageKey !== undefined) {
        this.languageService.changeLanguage(languageKey);
      }
    });
  }
}
