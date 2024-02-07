import { ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing'
import { LoginComponent } from './login.component'
import { ReactiveFormsModule } from '@angular/forms'
import { RouterTestingModule } from '@angular/router/testing'
import { LoginService } from '../service/login.service'
import { StateStorageService } from '../service/state-storage.service'
import { AccountService } from '../service/account.service'
import { of, throwError } from 'rxjs'

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
      declarations: [LoginComponent],
      imports: [ReactiveFormsModule, RouterTestingModule],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: StateStorageService, useValue: stateStorageServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
      ],
    }).compileComponents()

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
      })
    )

    component.loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    component.login()

    expect(component.showMfa).toBe(false)
    expect(component.authenticationError).toBe(false)
  })

  it('should handle MFA required', fakeAsync(() => {
    const mockLoginResult = { mfaRequired: true }
    loginService.login.and.returnValue(of(mockLoginResult))

    component.loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    component.login()

    tick() // Wait for Observable to emit

    expect(component.showMfa).toBe(true)
  }))

  it('should handle MFA code error', fakeAsync(() => {
    const mockLoginResult = { mfaRequired: true }
    loginService.login.and.returnValue(of(mockLoginResult))

    component.loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: 'invalidCode',
    })

    component.login()

    tick() // Wait for Observable to emit

    expect(component.showMfa).toBe(true)
    expect(component.mfaError).toBe(true)
  }))

  it('should handle login error', fakeAsync(() => {
    loginService.login.and.returnValue(throwError(() => new Error('error')))

    component.loginForm.patchValue({
      username: 'testuser',
      password: 'testpassword',
      mfaCode: '',
    })

    component.login()

    tick() // Wait for Observable to emit

    expect(component.authenticationError).toBe(true)
  }))
})
