import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { GatewaySharedModule } from 'app/shared';
import {
  AffiliationComponent,
  AffiliationDetailComponent,
  AffiliationUpdateComponent,
  AffiliationDeletePopupComponent,
  AffiliationDeleteDialogComponent,
  affiliationRoute,
  affiliationPopupRoute
} from './';

const ENTITY_STATES = [...affiliationRoute, ...affiliationPopupRoute];

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild(ENTITY_STATES)],
  declarations: [
    AffiliationComponent,
    AffiliationDetailComponent,
    AffiliationUpdateComponent,
    AffiliationDeleteDialogComponent,
    AffiliationDeletePopupComponent
  ],
  entryComponents: [AffiliationComponent, AffiliationUpdateComponent, AffiliationDeleteDialogComponent, AffiliationDeletePopupComponent],
  providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AssertionServicesAffiliationModule {
  constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
    this.languageHelper.language.subscribe((languageKey: string) => {
      if (languageKey !== undefined) {
        this.languageService.changeLanguage(languageKey);
      }
    });
  }
}
