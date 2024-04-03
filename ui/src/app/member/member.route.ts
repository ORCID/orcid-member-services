import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router'
import { AuthGuard } from '../account/auth.guard'
import { Observable, filter, of, take } from 'rxjs'
import { inject } from '@angular/core'
import { IMember, Member } from './model/member.model'
import { MemberService } from './service/member.service'
import { MembersComponent } from './members.component'

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
    path: 'members',
    component: MembersComponent,
    data: {
      authorities: ['ROLE_ADMIN'],
      defaultSort: 'id,asc',
      pageTitle: 'gatewayApp.msUserServiceMSMember.home.title.string',
    },
    canActivate: [AuthGuard],
  },
]
