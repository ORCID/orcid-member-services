import { ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing'
import { NavbarComponent } from './navbar.component'
import { ReactiveFormsModule } from '@angular/forms'
import { RouterTestingModule } from '@angular/router/testing'
import { of, throwError } from 'rxjs'
import { MemberService } from 'src/app/member/service/member.service'
import { AccountService, LoginService } from 'src/app/account'
import { By } from '@angular/platform-browser'
import { HasAnyAuthorityDirective } from 'src/app/shared/directive/has-any-authority.directive'

describe('NavbarComponent', () => {
  let component: NavbarComponent
  let fixture: ComponentFixture<NavbarComponent>
  let loginService: jasmine.SpyObj<LoginService>
  let accountService: jasmine.SpyObj<AccountService>
  let memberService: jasmine.SpyObj<MemberService>

  beforeEach(() => {
    const loginServiceSpy = jasmine.createSpyObj('LoginService', ['login', 'logout'])
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['find', 'setManagedMember'])
    const accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'isAuthenticated',
      'hasAnyAuthority',
      'getAuthenticationState',
      'isLoggedAs',
      'isOrganizationOwner',
      'getImageUrl',
      'getSalesforceId',
    ])

    TestBed.configureTestingModule({
      declarations: [NavbarComponent, HasAnyAuthorityDirective],
      imports: [ReactiveFormsModule, RouterTestingModule],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(NavbarComponent)
    component = fixture.componentInstance
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should display reports menu', fakeAsync(() => {
    accountService.isAuthenticated.and.returnValue(true)
    accountService.hasAnyAuthority.and.returnValue(true)
    accountService.isLoggedAs.and.returnValue(false)
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getSalesforceId.and.returnValue(null)
    accountService.getAuthenticationState.and.returnValue(
      of({
        activated: true,
        authorities: ['ROLE_USER'],
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
    fixture.detectChanges()
    tick()

    const debugElement = fixture.debugElement.query(By.css('#tools-menu'))
    expect(debugElement).toBeTruthy()
  }))

  /* it('should call loginService.login and handle successful login', fakeAsync(() => {
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
  
      tick() // Wait for Observable to emit
  
      expect(component.showMfa).toBe(false)
      expect(component.authenticationError).toBe(false)
    }))
  
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
    })) */
})
