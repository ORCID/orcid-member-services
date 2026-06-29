import { importProvidersFrom, NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'
import { LoginComponent } from './account'
import { routes as accountRoutes } from './account/account.route'
import { routes as homeRoutes } from './home/home.route'
import { errorRoutes } from './error/error.route'
import { navbarRoute } from './layout/navbar/navbar.route'

const routes: Routes = [
  {
    path: 'auth/callback',
    children: [],
  },
  ...accountRoutes,
  ...homeRoutes,
  {
    path: 'users',
    loadChildren: () => import('./user/user.route').then((m) => m.routes),
  },
  {
    path: 'affiliations',
    loadChildren: () => import('./affiliation/affiliation.route').then((m) => m.affiliationRoutes),
  },
  {
    path: 'landing-page',
    loadChildren: () => import('./landing-page/landing-page.route').then((m) => m.LANDING_PAGE_ROUTE),
    providers: [importProvidersFrom(BrowserAnimationsModule)],
  },
  {
    path: 'members',
    loadChildren: () => import('./member/member.route').then((m) => m.memberRoutes),
  },
  {
    path: 'report',
    loadChildren: () => import('./report/report.route').then((m) => [m.REPORT_ROUTE]),
  },
  {
    path: 'api-credentials',
    loadChildren: () => import('./api-credentials/api-credentials.route').then((m) => m.routes),
  },
  { path: 'login', component: LoginComponent },
]

@NgModule({
  imports: [RouterModule.forRoot([...routes, navbarRoute, ...errorRoutes])],
  exports: [RouterModule],
})
export class AppRoutingModule {}
