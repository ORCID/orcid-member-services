import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, Routes } from '@angular/router'
import { HomeComponent } from '../home/home.component'
import { AuthGuard } from '../account/auth.guard'
import { MemberInfoComponent } from './member-info/member-info.component'
import { Injectable, inject } from '@angular/core'
import { MemberService } from '../member/service/member.service'
import { Observable, map } from 'rxjs'
import { MemberInfoEditComponent } from './member-info/member-info-edit.component'

export const ManageMemberGuard = (route: ActivatedRouteSnapshot): Observable<boolean> | boolean => {
  const router = inject(Router)
  const memberService = inject(MemberService)

  return memberService.getManagedMember().pipe(
    map((salesforceId) => {
      if (salesforceId) {
        const segments = ['manage', salesforceId]

        if (route['routeConfig']?.path === 'edit') {
          segments.push('edit')
        }

        if (route['routeConfig']?.path === 'contact/new') {
          segments.push('contact', 'new')
        }

        if (route['routeConfig']?.path === 'contact/:contactId/edit') {
          segments.push('contact', route.params?.['contactId'], 'edit')
        }

        router.navigate(segments)
        return false
      }
      return true
    })
  )
}

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
    data: {
      authorities: ['ROLE_USER'],
      pageTitle: 'home.title.string',
    },
    canActivate: [AuthGuard],
    children: [
      {
        path: '',
        component: MemberInfoComponent,
        data: {
          authorities: ['ROLE_USER'],
          pageTitle: 'home.title.string',
        },
        canActivate: [ManageMemberGuard],
      },
      {
        path: 'manage/:id',
        component: MemberInfoComponent,
        data: {
          authorities: ['ROLE_USER', 'ROLE_CONSORTIUM_LEAD'],
          pageTitle: 'home.title.string',
        },
        canActivate: [AuthGuard],
      },
      {
        path: 'edit',
        component: MemberInfoEditComponent,
        data: {
          authorities: ['ROLE_USER'],
          pageTitle: 'home.title.string',
        },
        canActivate: [ManageMemberGuard],
      },
      {
        path: 'manage/:id/edit',
        component: MemberInfoEditComponent,
        data: {
          authorities: ['ROLE_USER'],
          pageTitle: 'home.title.string',
        },
        canActivate: [AuthGuard],
      },
    ],
  },
]
