import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { GatewaySharedModule } from 'app/shared';
import {
  MSUserComponent,
  MSUserDetailComponent,
  MSUserUpdateComponent,
  MSUserDeletePopupComponent,
  MSUserDeleteDialogComponent,
  MSUserImportPopupComponent,
  MSUserImportDialogComponent,
  msUserRoute,
  msUserPopupRoute
} from './';

import { MSUserOwnershipChangeDirective } from './ms-user-ownership-change.directive';

const ENTITY_STATES = [...msUserRoute, ...msUserPopupRoute];

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild(ENTITY_STATES)],
  declarations: [
    MSUserComponent,
    MSUserDetailComponent,
    MSUserUpdateComponent,
    MSUserDeleteDialogComponent,
    MSUserDeletePopupComponent,
    MSUserOwnershipChangeDirective,
    MSUserImportDialogComponent,
    MSUserImportPopupComponent
  ],
  entryComponents: [
    MSUserComponent,
    MSUserUpdateComponent,
    MSUserDeleteDialogComponent,
    MSUserDeletePopupComponent,
    MSUserImportDialogComponent,
    MSUserImportPopupComponent
  ],
  providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class MSUserServiceMSUserModule {
  constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
    this.languageHelper.language.subscribe((languageKey: string) => {
      if (languageKey !== undefined) {
        this.languageService.changeLanguage(languageKey);
      }
    });
  }
}
