import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot, Route, ResolveFn, RouterStateSnapshot } from '@angular/router'
import { Observable, of } from 'rxjs'
import { ReportComponent } from './report.component'

export const ReportResolver: ResolveFn<string | null> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
): Observable<string | null> => {
  const reportType = route.params['reportType'] ? route.params['reportType'] : null
  return of(reportType)
}

export const REPORT_ROUTE: Route = {
  path: ':reportType',
  component: ReportComponent,
  resolve: {
    reportType: ReportResolver,
  },
  data: {
    authorities: [],
    pageTitle: 'gatewayApp.report.member.title.string',
  },
}
