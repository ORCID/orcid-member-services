import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { GatewaySharedModule } from 'app/shared';
import {
  AssertionComponent,
  AssertionDetailComponent,
  AssertionUpdateComponent,
  AssertionDeletePopupComponent,
  AssertionDeleteDialogComponent,
  AssertionImportPopupComponent,
  AssertionImportDialogComponent,
  assertionRoute,
  assertionPopupRoute
} from './';

const ENTITY_STATES = [...assertionRoute, ...assertionPopupRoute];

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild(ENTITY_STATES)],
  declarations: [
    AssertionComponent,
    AssertionDetailComponent,
    AssertionUpdateComponent,
    AssertionDeleteDialogComponent,
    AssertionDeletePopupComponent,
    AssertionImportPopupComponent,
    AssertionImportDialogComponent
  ],
  entryComponents: [
    AssertionComponent,
    AssertionUpdateComponent,
    AssertionDeleteDialogComponent,
    AssertionDeletePopupComponent,
    AssertionImportPopupComponent,
    AssertionImportDialogComponent
  ],
  providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AssertionServiceAssertionModule {
  constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
    this.languageHelper.language.subscribe((languageKey: string) => {
      if (languageKey !== undefined) {
        this.languageService.changeLanguage(languageKey);
      }
    });
  }
}
