import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, Route } from '@angular/router';
import { ReportComponent } from './';
import { Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReportResolve implements Resolve<String> {
  constructor() {}

  resolve(route: ActivatedRouteSnapshot): Observable<String> {
    const reportType = route.params['reportType'] ? route.params['reportType'] : null;
    return of(reportType);
  }
}

export const REPORT_ROUTE: Route = {
  path: 'report/:reportType',
  component: ReportComponent,
  resolve: {
    reportType: ReportResolve
  },
  data: {
    authorities: [],
    pageTitle: 'gatewayApp.report.member.title.string'
  }
};
