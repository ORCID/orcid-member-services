import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GatewaySharedModule } from 'app/shared';
import { HOME_ROUTE, HomeComponent } from './';
import { RouterModule } from '@angular/router';
@NgModule({
  imports: [
    GatewaySharedModule,
    RouterModule.forChild([HOME_ROUTE]),
    CommonModule,
    ReactiveFormsModule,
    FormsModule
  ],
  declarations: [
    HomeComponent
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HomeModule { }
