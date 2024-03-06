import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { RouterModule } from '@angular/router'
import { ClipboardModule } from 'ngx-clipboard'
import { AffiliationsComponent } from './affiliations.component'
import { affiliationRoutes } from './affiliation.route'
import { SharedModule } from '../shared/shared.module'
import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'

@NgModule({
  imports: [CommonModule, FormsModule, SharedModule, RouterModule.forChild(affiliationRoutes), ClipboardModule],
  declarations: [AffiliationsComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class AffiliationModule {
  constructor() {}
}
