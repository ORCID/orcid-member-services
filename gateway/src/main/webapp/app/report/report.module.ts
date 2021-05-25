import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { GatewaySharedModule } from 'app/shared';
import { REPORT_ROUTE, ReportComponent } from './';

@NgModule({
  imports: [GatewaySharedModule, RouterModule.forChild([REPORT_ROUTE])],
  declarations: [ReportComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class GatewayReportModule {}
