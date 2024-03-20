import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { RouterModule } from '@angular/router'
import { ClipboardModule } from 'ngx-clipboard'
import { AffiliationsComponent } from './affiliations.component'
import { affiliationRoutes } from './affiliation.route'
import { SharedModule } from '../shared/shared.module'
import { CommonModule } from '@angular/common'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { AffiliationDetailComponent } from './affiliation-detail.component'
import {
  AffiliationImportDialogComponent,
  AffiliationImportPopupComponent,
} from './affiliation-import-dialog.component'
import { AffiliationDeleteDialogComponent, AffiliationDeletePopupComponent } from './affiliation-delete.component'
import { AffiliationUpdateComponent } from './affiliation-update.component'

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    RouterModule.forChild(affiliationRoutes),
    ClipboardModule,
  ],
  declarations: [
    AffiliationsComponent,
    AffiliationDetailComponent,
    AffiliationImportDialogComponent,
    AffiliationImportPopupComponent,
    AffiliationUpdateComponent,
    AffiliationDeleteDialogComponent,
    AffiliationDeletePopupComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class AffiliationModule {}
