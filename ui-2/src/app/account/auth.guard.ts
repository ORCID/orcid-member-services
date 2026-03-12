import { inject } from '@angular/core'
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { Observable, of } from 'rxjs'
import { map, switchMap, take, filter } from 'rxjs/operators' // <--- Import filter

import { StateStorageService } from './service/state-storage.service'
import { AccountService } from './service/account.service'

export const AuthGuard = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | boolean => {
  const authorities = route.data['authorities']

  const router = inject(Router)
  const accountService = inject(AccountService)
  const stateStorageService = inject(StateStorageService)
  const oidcSecurityService = inject(OidcSecurityService)

  return oidcSecurityService.checkAuth().pipe(
    take(1),
    switchMap(({ isAuthenticated }) => {
      // 1. If not authenticated at all, redirect to login
      if (!isAuthenticated) {
        if (state.url === '/login') {
          return of(true)
        }
        stateStorageService.storeUrl(state.url)
        router.navigate(['/login'])
        return of(false)
      }

      console.log('AuthGuard: Authenticated. Waiting for Account Data...')

      // 2. Fetch Account Data
      return accountService.getAccountData().pipe(
        // This ensures we don't fail while the HTTP request is still loading.
        filter((account) => account !== undefined),
        take(1), // Take the first valid result and complete

        map((account) => {
          if (account) {
            const hasAnyAuthority = accountService.hasAnyAuthority(authorities)
            if (hasAnyAuthority) {
              return true
            } else {
              router.navigate(['accessdenied'])
              return false
            }
          } else {
            // If account is null (API failed or 401), send to login
            router.navigate(['/login'])
            return false
          }
        })
      )
    })
  )
}
