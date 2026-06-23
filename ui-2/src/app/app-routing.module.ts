import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'
import { LoginComponent } from './account'
import { errorRoutes } from './error/error.route'
import { navbarRoute } from './layout/navbar/navbar.route'

const routes: Routes = [
  {
    path: 'auth/callback',
    children: [],
  },
  {
    path: '',
    loadChildren: () => import('./account/account.module').then((m) => m.AccountModule),
  },
  {
    path: '',
    loadChildren: () => import('./home/home.module').then((m) => m.HomeModule),
  },
  {
    path: 'users',
    loadChildren: () => import('./user/user.module').then((m) => m.UserModule),
  },
  {
    path: 'affiliations',
    loadChildren: () => import('./affiliation/affiliation.module').then((m) => m.AffiliationModule),
  },
  {
    path: 'landing-page',
    loadChildren: () => import('./landing-page/landing-page.module').then((m) => m.LandingPageModule),
  },
  {
    path: 'members',
    loadChildren: () => import('./member/member.module').then((m) => m.MemberModule),
  },
  {
    path: 'report',
    loadChildren: () => import('./report/report.module').then((m) => m.ReportModule),
  },
  {
    path: 'api-credentials',
    loadChildren: () => import('./api-credentials/api-credentials.module').then((m) => m.ApiCredentialsModule),
  },
  { path: 'login', component: LoginComponent },
]

@NgModule({
  imports: [RouterModule.forRoot([...routes, navbarRoute, ...errorRoutes])],
  exports: [RouterModule],
})
export class AppRoutingModule {}
