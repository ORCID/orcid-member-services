import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { RouterModule } from '@angular/router'
import { ReportComponent } from './report.component'
import { REPORT_ROUTE } from './report.route'
import { CommonModule } from '@angular/common'

@NgModule({
  imports: [CommonModule, RouterModule.forChild([REPORT_ROUTE])],
  declarations: [ReportComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ReportModule {}
