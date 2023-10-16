import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { navbarRoute } from './layout/navbar/navbar.route';

const routes: Routes = [
  {
    path: '',
    loadChildren: () =>
      import('./account/account.module').then(m => m.AccountModule)
  }
] 

@NgModule({
  imports: [RouterModule.forRoot([...routes, navbarRoute])],
  exports: [RouterModule]
})
export class AppRoutingModule { }
