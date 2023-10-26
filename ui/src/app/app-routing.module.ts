import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'
import { navbarRoute } from './layout/navbar/navbar.route'

const routes: Routes = [
  {
    path: '',
    loadChildren: () =>
      import('./account/account.module').then((m) => m.AccountModule),
  },
  {
    path: '',
    loadChildren: () => import('./home/home.module').then((m) => m.HomeModule),
  },
]

@NgModule({
  imports: [RouterModule.forRoot([...routes, navbarRoute])],
  exports: [RouterModule],
})
export class AppRoutingModule {}
