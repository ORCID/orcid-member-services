import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router'
import { AuthGuard } from '../account/auth.guard'
import { Observable, filter, of, take } from 'rxjs'
import { inject } from '@angular/core'
import { IMember, Member } from './model/member.model'
import { MemberService } from './service/member.service'
import { MembersComponent } from './members.component'
import { MemberUpdateComponent } from './member-update.component'
import { MemberDetailComponent } from './member-detail.component'
import { MemberImportPopupComponent } from './member-import-dialog.component'

export const MemberResolver: ResolveFn<Member | null> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  memberService: MemberService = inject(MemberService)
): Observable<IMember | null> => {
  if (route.paramMap.get('id')) {
    return memberService.find(route.paramMap.get('id')!).pipe(
      filter<IMember>((member: IMember) => !!member),
      take(1)
    )
  } else {
    return of(null)
  }
}

export const memberRoutes: Routes = [
  {
    path: '',
    component: MembersComponent,
    data: {
      authorities: ['ROLE_ADMIN'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string',
    },
    canActivate: [AuthGuard],
    children: [
      {
        path: 'import',
        component: MemberImportPopupComponent,
        data: {
          authorities: ['ROLE_ADMIN'],
          pageTitle: 'gatewayApp.msUserServiceMSUser.home.title.strings',
        },
        canActivate: [AuthGuard],
        outlet: 'popup',
      },
    ],
  },
  {
    path: 'new',
    component: MemberUpdateComponent,
    resolve: {
      member: MemberResolver,
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: ':id/edit',
    component: MemberUpdateComponent,
    resolve: {
      member: MemberResolver,
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string',
    },
    canActivate: [AuthGuard],
  },
  {
    path: ':id/view',
    component: MemberDetailComponent,
    resolve: {
      member: MemberResolver,
    },
    data: {
      authorities: ['ROLE_ADMIN'],
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string',
    },
    canActivate: [AuthGuard],
  },
]
