import { TestBed } from '@angular/core/testing'
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { Observable, of } from 'rxjs'
import { AuthGuard } from './auth.guard'
import { IAccount } from './model/account.model'
import { AccountService } from './service/account.service'
import { StateStorageService } from './service/state-storage.service'

describe('AuthGuard', () => {
  let accountService: jasmine.SpyObj<AccountService>
  let router: jasmine.SpyObj<Router>
  let isAuthenticated: boolean

  const apiCredentialsRouteData = { authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER'], allowManageApiCredentials: true }

  const runGuard = (data: Record<string, unknown>): boolean => {
    const route = { data } as unknown as ActivatedRouteSnapshot
    const state = { url: '/api-credentials/memberId' } as RouterStateSnapshot
    let result = false
    const guardResult = TestBed.runInInjectionContext(() => AuthGuard(route, state)) as Observable<boolean>
    guardResult.subscribe((value) => (result = value))
    return result
  }

  beforeEach(() => {
    isAuthenticated = true
    accountService = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'hasAnyAuthority',
      'isManageApiCredentialsEnabled',
    ])
    accountService.getAccountData.and.returnValue(of({ id: 'id' } as IAccount))
    accountService.hasAnyAuthority.and.returnValue(false)
    accountService.isManageApiCredentialsEnabled.and.returnValue(false)
    router = jasmine.createSpyObj('Router', ['navigate'])

    TestBed.configureTestingModule({
      providers: [
        { provide: AccountService, useValue: accountService },
        { provide: Router, useValue: router },
        { provide: StateStorageService, useValue: jasmine.createSpyObj('StateStorageService', ['storeUrl']) },
        { provide: OidcSecurityService, useValue: { checkAuth: () => of({ isAuthenticated }) } },
      ],
    })
  })

  it('should allow a user with one of the route authorities', () => {
    accountService.hasAnyAuthority.and.returnValue(true)

    expect(runGuard(apiCredentialsRouteData)).toBeTrue()
    expect(accountService.hasAnyAuthority).toHaveBeenCalledWith(['ROLE_ADMIN', 'ROLE_ORG_OWNER'])
    expect(router.navigate).not.toHaveBeenCalled()
  })

  it('should allow a user with the Manage API credentials permission on routes that opt in', () => {
    accountService.isManageApiCredentialsEnabled.and.returnValue(true)

    expect(runGuard(apiCredentialsRouteData)).toBeTrue()
    expect(router.navigate).not.toHaveBeenCalled()
  })

  it('should deny a user with the Manage API credentials permission on routes that do not opt in', () => {
    accountService.isManageApiCredentialsEnabled.and.returnValue(true)

    expect(runGuard({ authorities: ['ROLE_ADMIN', 'ROLE_ORG_OWNER'] })).toBeFalse()
    expect(router.navigate).toHaveBeenCalledWith(['accessdenied'])
  })

  it('should deny a user with neither the route authorities nor the permission', () => {
    expect(runGuard(apiCredentialsRouteData)).toBeFalse()
    expect(router.navigate).toHaveBeenCalledWith(['accessdenied'])
  })

  it('should redirect unauthenticated users to login', () => {
    isAuthenticated = false

    expect(runGuard(apiCredentialsRouteData)).toBeFalse()
    expect(router.navigate).toHaveBeenCalledWith(['/login'])
  })
})
