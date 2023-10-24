import { Routes } from "@angular/router";
import { HomeComponent } from "../home/home.component";
import { AuthGuard } from "../account/auth.guard";

export const routes: Routes = [
    { 

        path: '',
        component: HomeComponent,
        data: {
          authorities: ['ROLE_USER'],
          pageTitle: 'home.title.string'
        },
        canActivate: [AuthGuard]
    
    }
  ];
  