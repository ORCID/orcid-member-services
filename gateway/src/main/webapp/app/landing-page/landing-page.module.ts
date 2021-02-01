import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { GatewaySharedModule } from 'app/shared';
import { LANDING_PAGE_ROUTE, LandingPageComponent } from './';
import { BrowserModule } from '@angular/platform-browser';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@NgModule({
  imports: [
    GatewaySharedModule,
    RouterModule.forChild([LANDING_PAGE_ROUTE]),
    BrowserModule,
    BrowserAnimationsModule,

    MatProgressSpinnerModule,
    MatProgressBarModule
  ],
  declarations: [LandingPageComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayLandingPageModule {}
