import { NgModule } from '@angular/core'
import { ActivatedRouteSnapshot, Resolve, Route, RouterModule, RouterStateSnapshot, Routes } from '@angular/router'
import { navbarRoute } from './layout/navbar/navbar.route'
import { errorRoutes } from './error/error.route'
import { AuthGuard } from './account/auth.guard'
import { UsersComponent } from './user/users.component'

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
]

@NgModule({
  imports: [RouterModule.forRoot([...routes, navbarRoute, ...errorRoutes])],
  exports: [RouterModule],
})
export class AppRoutingModule {}
