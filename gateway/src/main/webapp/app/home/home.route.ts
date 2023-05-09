import { ActivatedRouteSnapshot, Resolve, Route, RouterStateSnapshot } from '@angular/router';
import { UserRouteAccessService } from 'app/core';

import { HomeComponent } from './';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';
import { ContactUpdateComponent } from './member-info-landing/contact-update/contact-update.component';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ISFMemberContact, SFMemberContact } from 'app/shared/model/salesforce-member-contact.model';
import { map } from 'rxjs/operators';
import { MSMemberService } from 'app/entities/member';

@Injectable({ providedIn: 'root' })
export class ContactResolve implements Resolve<any> {
  constructor(private service: MSMemberService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ISFMemberContact> {
    const id = route.params['id'] ? route.params['id'] : null;
    // TODO: needs to be replaced with the upcoming /contact endpoint (get)
    if (id) {
      return this.service.getMemberContacts().pipe(
        map(data => {
          if (data) {
            return Object.values(data).find(contact => contact.contactEmail == id);
          }
        })
      );
    }
    return of(new SFMemberContact());
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
      resolve: {
        contact: ContactResolve
      },
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    }
  ]
};
