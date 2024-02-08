import { inject } from '@angular/core'
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router'

import { StateStorageService } from './service/state-storage.service'
import { AccountService } from './service/account.service'
import { Observable, filter, map } from 'rxjs'

export const AuthGuard = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | boolean => {
  const authorities = route.data['authorities']

  const router = inject(Router)
  const accountService = inject(AccountService)
  const stateStorageService = inject(StateStorageService)

  return accountService.getAccountData().pipe(
    filter((account) => account !== undefined),
    map((account) => {
      if (account) {
        const hasAnyAuthority = accountService.hasAnyAuthority(authorities)
        if (hasAnyAuthority) {
          return true
        } else {
          if (state.url === '/login') {
            router.navigate(['/'])
          } else {
            router.navigate(['accessdenied'])
          }
          return false
        }
      } else {
        if (state.url === '/login') {
          return true
        }

        router.navigate(['/login'])
        stateStorageService.storeUrl(state.url)
        return false
      }
    })
  )
}
