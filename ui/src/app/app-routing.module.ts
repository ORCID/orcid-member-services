import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'
import { navbarRoute } from './layout/navbar/navbar.route'
import { errorRoutes } from './error/error.route'

const routes: Routes = [
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
]

@NgModule({
  imports: [RouterModule.forRoot([...routes, navbarRoute, ...errorRoutes])],
  exports: [RouterModule],
})
export class AppRoutingModule {}
