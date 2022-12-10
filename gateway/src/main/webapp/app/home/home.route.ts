import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Route, RouterStateSnapshot } from '@angular/router';
import { AccountService, UserRouteAccessService } from 'app/core';
import { SFMemberData } from 'app/shared/model/salesforce-member-data.model';
import { Observable, of } from 'rxjs';

import { HomeComponent } from './';
import { MemberInfoEditComponent } from './member-info-landing/member-info-edit/member-info-edit.component';
import { MemberInfoLandingComponent } from './member-info-landing/member-info-landing.component';

@Injectable({ providedIn: 'root' })
export class MemberDataResolve implements Resolve<SFMemberData> {
  constructor(private service: AccountService) {}

  async resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<SFMemberData> {
    return this.service.getCurrentMemberData().then(data => {
      if (data.value) {
        return data.value;
      } else {
        return null;
      }
    });
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
      component: MemberInfoLandingComponent
    },
    {
      path: 'edit',
      component: MemberInfoEditComponent,
      resolve: {
        data: MemberDataResolve
      },
      canActivate: [UserRouteAccessService]
    }
  ]
};
