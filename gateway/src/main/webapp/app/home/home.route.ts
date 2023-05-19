import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core';

import { HomeComponent } from './';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';
import { ContactUpdateComponent } from './member-info-landing/contact-update/contact-update.component';
import { ConsortiumMemberAddComponent } from './member-info-landing/consortium-members/consortium-member-add.component';

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
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      }
    },
    {
      path: 'edit',
      component: MemberInfoEditComponent,
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'contact/new',
      component: ContactUpdateComponent,
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'contact/:id/edit',
      component: ContactUpdateComponent,
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'consortium-member/add',
      component: ConsortiumMemberAddComponent,
      data: {
        authorities: ['ROLE_USER', 'ROLE_CONSORTIUM_LEAD'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    }
  ]
};
