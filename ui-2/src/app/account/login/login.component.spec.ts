/// <reference types="jasmine" />

import { ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing'
import { LoginComponent } from './login.component'
import { ReactiveFormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { LoginService } from '../service/login.service'
import { StateStorageService } from '../service/state-storage.service'
import { AccountService } from '../service/account.service'
import { of, throwError } from 'rxjs'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { OidcSecurityServiceMock } from 'src/app/shared/service/oidc-security-service-mock'

describe('LoginComponent', () => {
  let component: LoginComponent
  let fixture: ComponentFixture<LoginComponent>
  let loginService: jasmine.SpyObj<LoginService>
  let stateStorageService: jasmine.SpyObj<StateStorageService>
  let accountService: jasmine.SpyObj<AccountService>

  beforeEach(() => {
    const loginServiceSpy = jasmine.createSpyObj('LoginService', ['login', 'logout'])
    const stateStorageServiceSpy = jasmine.createSpyObj('StateStorageService', ['getUrl', 'storeUrl'])
    const accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])

    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, RouterModule.forRoot([]), LoginComponent],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: StateStorageService, useValue: stateStorageServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: OidcSecurityService, useClass: OidcSecurityServiceMock },
      ],
    })

    fixture = TestBed.createComponent(LoginComponent)
    component = fixture.componentInstance
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>
    stateStorageService = TestBed.inject(StateStorageService) as jasmine.SpyObj<StateStorageService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call loginService.login and handle successful login', () => {
    const mockLoginResult = { mfaRequired: false }
    loginService.login.and.returnValue(of(mockLoginResult))
    accountService.getAccountData.and.returnValue(
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
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )

    ;(component as any).loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    ;(component as any).login()

    expect((component as any).showMfa).toBe(false)
    expect((component as any).authenticationError).toBe(false)
  })

  it('should handle MFA required', fakeAsync(() => {
    const mockError = {
      status: 401,
      error: { error: 'mfa_required' },
    }
    loginService.login.and.returnValue(throwError(() => mockError))

    ;(component as any).loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    ;(component as any).login()

    tick()

    expect((component as any).showMfa).toBe(true)
    expect((component as any).authenticationError).toBe(false)
  }))

  it('should set authenticationError when 401 is returned without mfa error', fakeAsync(() => {
    const mockError = {
      status: 401,
      error: { error: 'bad_credentials' },
    }
    loginService.login.and.returnValue(throwError(() => mockError))

    ;(component as any).loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    ;(component as any).login()

    tick()

    expect((component as any).showMfa).toBe(false)
    expect((component as any).authenticationError).toBe(true)
  }))

  it('should handle MFA code error', fakeAsync(() => {
    const mockError = {
      status: 401,
      error: { error: 'mfa_required' },
    }
    loginService.login.and.returnValue(throwError(() => mockError))

    ;(component as any).loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    ;(component as any).login()

    tick() // Wait for Observable to emit

    expect((component as any).showMfa).toBe(true)
    expect((component as any).mfaError).toBe(false)
  }))

  it('should handle MFA invalid code error', fakeAsync(() => {
    const mockError = {
      status: 401,
      error: { error: 'mfa_invalid' },
    }
    loginService.login.and.returnValue(throwError(() => mockError))

    ;(component as any).loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: 'invalidcode',
    })

    ;(component as any).login()

    tick() // Wait for Observable to emit

    expect((component as any).showMfa).toBe(true)
    expect((component as any).mfaError).toBe(true)
  }))

  it('should handle login error', fakeAsync(() => {
    loginService.login.and.returnValue(throwError(() => new Error('error')))

    ;(component as any).loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    ;(component as any).login()

    tick() // Wait for Observable to emit

    expect((component as any).authenticationError).toBe(true)
  }))
})
