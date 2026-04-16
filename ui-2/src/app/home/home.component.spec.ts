import { ComponentFixture, TestBed } from '@angular/core/testing'

import { HomeComponent } from './home.component'
import { AccountService } from '../account'
import { of } from 'rxjs'
import { HttpClientTestingModule } from '@angular/common/http/testing'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { IAccount } from '../account/model/account.model'

describe('HomeComponent', () => {
  let component: HomeComponent
  let fixture: ComponentFixture<HomeComponent>
  let accountServiceSpy: jasmine.SpyObj<AccountService>

  beforeEach(() => {
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])

    const mockAccount: IAccount = {
      id: 'test-id',
      activated: true,
      authorities: ['ROLE_USER'],
      email: 'test@email.com',
      firstName: 'Test',
      langKey: 'en',
      lastName: 'User',
      imageUrl: '',
      salesforceId: 'test2',
      loggedAs: false,
      loginAs: '',
      mainContact: false,
      mfaEnabled: false,
      manageApiCredsEnabled: false,
    }

    accountServiceSpy.getAccountData.and.returnValue(of(mockAccount))

    const mockOidcSecurityService = {
      checkAuth: () => of({ isAuthenticated: true, userData: { email: 'test@email.com' } }),
      userData$: of({ email: 'test@email.com' }),
      isAuthenticated$: of(true),
      logoff: jasmine.createSpy('logoff'),
    }

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [HomeComponent],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: OidcSecurityService, useValue: mockOidcSecurityService },
      ],
    })
    fixture = TestBed.createComponent(HomeComponent)
    component = fixture.componentInstance

    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
  })

  it('should call getAccountData but not getMemberData', () => {
    accountServiceSpy.getAccountData.and.returnValue(of(null))

    expect(component).toBeTruthy()

    component.ngOnInit()

    expect(accountServiceSpy.getAccountData).toHaveBeenCalled()
    expect(component.loggedInMessage).toBeUndefined()
  })

  it('should call getMemberData if account data is not null', () => {
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: false,
        manageApiCredsEnabled: false
      })
    )

    expect(component).toBeTruthy()

    component.ngOnInit()

    expect(accountServiceSpy.getAccountData).toHaveBeenCalled()
    expect(component.loggedInMessage).toEqual('You are logged in as user email@email.com')
  })
})
