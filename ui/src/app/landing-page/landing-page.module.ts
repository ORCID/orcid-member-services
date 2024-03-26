import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { RouterModule } from '@angular/router'

import { BrowserModule } from '@angular/platform-browser'

import { BrowserAnimationsModule } from '@angular/platform-browser/animations'

import { MatProgressSpinnerModule } from '@angular/material/progress-spinner'
import { MatProgressBarModule } from '@angular/material/progress-bar'
import { LandingPageComponent } from './landing-page.component'
import { LANDING_PAGE_ROUTE } from './landing-page.route'

@NgModule({
  imports: [
    RouterModule.forChild([LANDING_PAGE_ROUTE]),
    BrowserModule,
    BrowserAnimationsModule,

    MatProgressSpinnerModule,
    MatProgressBarModule,
  ],
  declarations: [LandingPageComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class GatewayLandingPageModule {}
