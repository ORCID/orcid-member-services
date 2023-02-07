import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core';

import { HomeComponent } from './';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';

export const HOME_ROUTE: Route = {
  path: '',
  component: HomeComponent,
  data: {
    authorities: [],
    pageTitle: 'home.title.string'
  },
  children: [
    {
      path: '',
      component: MemberInfoLandingComponent,
      data: {
        pageTitle: 'home.title.string'
      }
    },
    {
      path: 'edit',
      component: MemberInfoEditComponent,
      data: {
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    }
  ]
};
