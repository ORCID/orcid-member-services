import { ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing'
import { NavbarComponent } from './navbar.component'
import { ReactiveFormsModule } from '@angular/forms'
import { RouterTestingModule } from '@angular/router/testing'
import { of, throwError } from 'rxjs'
import { MemberService } from 'src/app/member/service/member.service'
import { AccountService, LoginService } from 'src/app/account'
import { By } from '@angular/platform-browser'
import { HasAnyAuthorityDirective } from 'src/app/shared/directive/has-any-authority.directive'
import { HttpResponse } from '@angular/common/http'
import { Member } from 'src/app/member/model/member.model'

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
    accountService.hasAnyAuthority.and.returnValue(false)
    accountService.hasAnyAuthority.withArgs(['ROLE_USER']).and.returnValue(true)
    accountService.isLoggedAs.and.returnValue(false)
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getSalesforceId.and.returnValue('sfid')
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
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: false }))

    fixture.detectChanges()
    tick()

    const debugElement = fixture.debugElement.query(By.css('#tools-menu'))
    expect(debugElement).toBeTruthy()

    const consortiaReportLink = fixture.debugElement.query(By.css('#consortiaReportLink'))
    expect(consortiaReportLink).toBeFalsy()

    const consortiumMemberAffiliationsReportLink = fixture.debugElement.query(
      By.css('#consortiumMemberAffiliationsReportLink')
    )
    expect(consortiumMemberAffiliationsReportLink).toBeFalsy()

    const affiliationManagerLink = fixture.debugElement.query(By.css('#affiliationManagerLink'))
    expect(affiliationManagerLink).toBeFalsy()
  }))

  it('should display consortia report link', fakeAsync(() => {
    accountService.isAuthenticated.and.returnValue(true)
    accountService.hasAnyAuthority.and.returnValue(false)
    accountService.hasAnyAuthority.withArgs(['ROLE_CONSORTIUM_LEAD']).and.returnValue(true)
    accountService.hasAnyAuthority.withArgs(['ROLE_USER']).and.returnValue(true)
    accountService.isLoggedAs.and.returnValue(false)
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getSalesforceId.and.returnValue('sfid')
    accountService.getAuthenticationState.and.returnValue(
      of({
        activated: true,
        authorities: ['ROLE_USER', 'ROLE_CONSORTIUM_LEAD'],
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
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: true }))
    fixture.detectChanges()
    tick()

    const consortiaReportLink = fixture.debugElement.query(By.css('#consortiaReportLink'))
    expect(consortiaReportLink).toBeTruthy()

    const consortiumMemberAffiliationsReportLink = fixture.debugElement.query(
      By.css('#consortiumMemberAffiliationsReportLink')
    )
    expect(consortiumMemberAffiliationsReportLink).toBeTruthy()
  }))

  it('should display the affiliation manager link', fakeAsync(() => {
    accountService.isAuthenticated.and.returnValue(true)
    accountService.hasAnyAuthority.and.returnValue(false)
    accountService.hasAnyAuthority.withArgs(['ASSERTION_SERVICE_ENABLED']).and.returnValue(true)
    accountService.hasAnyAuthority.withArgs(['ROLE_USER']).and.returnValue(true)
    accountService.isLoggedAs.and.returnValue(false)
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getSalesforceId.and.returnValue('sfid')
    accountService.getAuthenticationState.and.returnValue(
      of({
        activated: true,
        authorities: ['ROLE_USER', 'ASSERTION_SERVICE_ENABLED'],
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
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: true }))
    fixture.detectChanges()
    tick()

    const affiliationManagerLink = fixture.debugElement.query(By.css('#affiliationManagerLink'))
    expect(affiliationManagerLink).toBeTruthy()
  }))
})
