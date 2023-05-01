import { ActivatedRouteSnapshot, Resolve, Route, RouterStateSnapshot } from '@angular/router';
import { AccountService, UserRouteAccessService } from 'app/core';

import { HomeComponent } from './';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';
import { ContactAddComponent } from './member-info-landing/contact-add/contact-add.component';
import { ContactEditComponent } from './member-info-landing/contact-edit/contact-edit.component';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ISFMemberContact, SFMemberContact } from 'app/shared/model/salesforce-member-contact.model';
import { filter } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ContactResolve implements Resolve<any> {
  constructor(private service: AccountService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ISFMemberContact> {
    const id = route.params['id'] ? route.params['id'] : null;
    console.log(id);

    if (id) {
      this.service.memberData.subscribe(data => {
        console.log(data);

        if (data && data.contacts) {
          console.log(data.contacts.find(contact => contact.contactEmail === data));

          return data.contacts.find(contact => contact.contactEmail === data);
        }
      });
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
      resolve: {
        contact: ContactResolve
      },
      data: {
        pageTitle: 'home.title.string'
      },
      canActivate: [UserRouteAccessService]
    }
  ]
};
