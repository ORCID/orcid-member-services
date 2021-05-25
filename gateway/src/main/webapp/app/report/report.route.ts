import { Route } from '@angular/router';

import { ReportComponent } from './';

export const REPORT_ROUTE: Route = {
  path: 'report',
  component: ReportComponent,
  data: {
    authorities: [],
    pageTitle: 'report.title.string'
  }
};
