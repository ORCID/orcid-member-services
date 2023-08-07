import { ActivatedRouteSnapshot, CanActivate, Route, Router } from '@angular/router';
import { UserRouteAccessService } from 'app/core';

import { HomeComponent } from './';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';
import { ContactUpdateComponent } from './member-info-landing/contact-update/contact-update.component';
import { RemoveConsortiumMemberComponent } from './member-info-landing/consortium-members/remove-consortium-member.component';
import { AddConsortiumMemberComponent } from './member-info-landing/consortium-members/add-consortium-member.component';
import { MSMemberService } from 'app/entities/member';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ManageMemberGuard implements CanActivate {
  constructor(private router: Router, private memberService: MSMemberService) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.memberService.getManagedMember().pipe(
      map(salesforceId => {
        if (salesforceId) {
          const segments = ['manage', salesforceId];

          if (route.routeConfig.path === 'edit') {
            segments.push('edit');
          }

          if (route.routeConfig.path === 'contact/new') {
            segments.push('contact', 'new');
          }

          if (route.routeConfig.path === 'contact/:contactId/edit') {
            segments.push('contact', route.params.contactId, 'edit');
          }

          this.router.navigate(segments);
          return false;
        }
        return true;
      })
    );
  }
}

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
      },
      canActivate: [ManageMemberGuard]
    },
    {
      path: 'manage/:id',
      component: MemberInfoLandingComponent,
      data: {
        authorities: ['ROLE_USER', 'ROLE_CONSORTIUM_LEAD'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'edit',
      component: MemberInfoEditComponent,
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService, ManageMemberGuard]
    },
    {
      path: 'manage/:id/edit',
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
      canActivate: [UserRouteAccessService, ManageMemberGuard]
    },
    {
      path: 'manage/:id/contact/new',
      component: ContactUpdateComponent,
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'contact/:contactId/edit',
      component: ContactUpdateComponent,
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService, ManageMemberGuard]
    },
    {
      path: 'manage/:id/contact/:contactId/edit',
      component: ContactUpdateComponent,
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'consortium-member/new',
      component: AddConsortiumMemberComponent,
      data: {
        authorities: ['ROLE_USER', 'ROLE_CONSORTIUM_LEAD'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'consortium-member/:id/remove',
      component: RemoveConsortiumMemberComponent,
      data: {
        authorities: ['ROLE_USER', 'ROLE_CONSORTIUM_LEAD'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    }
  ]
};
