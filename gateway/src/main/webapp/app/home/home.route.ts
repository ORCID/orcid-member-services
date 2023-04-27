import { ActivatedRouteSnapshot, Resolve, Route, RouterStateSnapshot } from '@angular/router';
import { UserRouteAccessService } from 'app/core';

import { HomeComponent } from './';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';
import { ContactAddComponent } from './member-info-landing/contact-add/contact-add.component';
import { ContactEditComponent } from './member-info-landing/contact-edit/contact-edit.component';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/* @Injectable({ providedIn: 'root' })
export class ContactResolve implements Resolve<IMSMember> {
  constructor(private service: MSMemberService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IMSMember> {
    const id = route.params['id'] ? route.params['id'] : null;
    if (id) {
      return this.service.find(id).pipe(
        filter((response: HttpResponse<MSMember>) => response.ok),
        map((msMember: HttpResponse<MSMember>) => msMember.body)
      );
    }
    return of(new MSMember());
  }
} */

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
    },
    {
      path: 'contact/new',
      component: ContactAddComponent,
      data: {
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    },
    {
      path: 'contact/:id/edit',
      component: ContactEditComponent,
      data: {
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    }
  ]
};
