import { CanActivate, Route, Router } from '@angular/router';
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

  canActivate(): Observable<boolean> {
    return this.memberService.getManagedMember().pipe(
      map(salesforceId => {
        if (salesforceId) {
          this.router.navigate(['manage', salesforceId]);
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
