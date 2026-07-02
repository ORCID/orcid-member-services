import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing'
import { ReactiveFormsModule } from '@angular/forms'
import { By } from '@angular/platform-browser'
import { of } from 'rxjs'
import { AccountService, LoginService } from 'src/app/account'
import { MemberService } from 'src/app/member/service/member.service'
import { HasAnyAuthorityDirective } from 'src/app/shared/directive/has-any-authority.directive'
import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { ApiCredentialsMfaEnabledDialogComponent } from './api-credentials-mfa-enabled-dialog/api-credentials-mfa-enabled-dialog.component'
import { NavbarComponent } from './navbar.component'
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { FeatureToggleService } from 'src/app/shared/service/feature-toggle.service'
import { RouterModule } from '@angular/router'

describe('NavbarComponent', () => {
  let component: NavbarComponent
  let fixture: ComponentFixture<NavbarComponent>
  let loginService: jasmine.SpyObj<LoginService>
  let accountService: jasmine.SpyObj<AccountService>
  let memberService: jasmine.SpyObj<MemberService>
  let modalService: jasmine.SpyObj<NgbModal>
  let featureToggleService: jasmine.SpyObj<FeatureToggleService>

  beforeEach(() => {
    spyOn(console, 'warn').and.stub()

    const featureToggleSpy = jasmine.createSpyObj('FeatureToggleService', ['isEnabled', 'initFeatures'])
    featureToggleSpy.initFeatures.and.returnValue(of(null))
    const loginServiceSpy = jasmine.createSpyObj('LoginService', ['login', 'logout'])
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['find', 'setManagedMember'])
    const accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'isAuthenticated',
      'hasAnyAuthority',
      'isLoggedAs',
      'isOrganizationOwner',
      'isManageApiCredentialsEnabled',
      'isMFAEnabled',
      'getImageUrl',
      'getMemberId',
    ])
    accountServiceSpy.getAccountData.and.returnValue(of(null))
    const modalServiceSpy = jasmine.createSpyObj('NgbModal', ['open'])
    const mockOidcSecurityService = {
      checkAuth: () => of({ isAuthenticated: true, userData: { email: 'test@email.com' } }),
      userData$: of({ email: 'test@email.com' }),
      isAuthenticated$: of({ isAuthenticated: true }),
      logoff: jasmine.createSpy('logoff'),
    }

    TestBed.configureTestingModule({
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [
        ReactiveFormsModule,
        RouterModule.forRoot([{ path: 'api-credentials', children: [] }]),
        NavbarComponent,
        HasAnyAuthorityDirective,
      ],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: NgbModal, useValue: modalServiceSpy },
        { provide: OidcSecurityService, useValue: mockOidcSecurityService },
        { provide: FeatureToggleService, useValue: featureToggleSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents()

    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    modalService = TestBed.inject(NgbModal) as jasmine.SpyObj<NgbModal>
    featureToggleService = TestBed.inject(FeatureToggleService) as jasmine.SpyObj<FeatureToggleService>
  })

  function createComponent() {
    fixture = TestBed.createComponent(NavbarComponent)
    component = fixture.componentInstance
  }

  it('should create', () => {
    createComponent()
    expect(component).toBeTruthy()
  })

  it('should display reports menu', fakeAsync(() => {
    accountService.isAuthenticated.and.returnValue(true)
    accountService.hasAnyAuthority.and.returnValue(false)
    accountService.hasAnyAuthority.withArgs(['ROLE_USER']).and.returnValue(true)
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.isManageApiCredentialsEnabled.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getMemberId.and.returnValue('memberId')
    accountService.getAccountData.and.returnValue(
      of({
        id: 'id',
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
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: false }))

    createComponent()
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
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getMemberId.and.returnValue('memberId')
    accountService.getAccountData.and.returnValue(
      of({
        id: 'id',
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
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: true }))
    createComponent()
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
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getMemberId.and.returnValue('memberId')
    accountService.getAccountData.and.returnValue(
      of({
        id: 'id',
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
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: true }))
    createComponent()
    fixture.detectChanges()
    tick()

    const affiliationManagerLink = fixture.debugElement.query(By.css('#affiliationManagerLink'))
    expect(affiliationManagerLink).toBeTruthy()
  }))

  it('should display the manage API credentials link', fakeAsync(() => {
    featureToggleService.isEnabled.withArgs('MANAGE_API_CREDENTIALS').and.returnValue(true)

    accountService.isAuthenticated.and.returnValue(true)
    accountService.hasAnyAuthority.and.returnValue(false)
    accountService.hasAnyAuthority.withArgs(['ROLE_USER']).and.returnValue(true)
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.isManageApiCredentialsEnabled.and.returnValue(true)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['ROLE_USER'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        memberId: 'memberId',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: false,
        manageApiCredsEnabled: true,
      })
    )
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: false }))
    createComponent()
    fixture.detectChanges()
    tick()

    const manageApiCredentialsLink = fixture.debugElement.query(By.css('#manageApiCredentialsLink'))
    expect(manageApiCredentialsLink).toBeTruthy()
  }))

  it('should open MFA dialog when manage API credentials is clicked and MFA is not enabled', fakeAsync(() => {
    featureToggleService.isEnabled.withArgs('MANAGE_API_CREDENTIALS').and.returnValue(true)

    accountService.isAuthenticated.and.returnValue(true)
    accountService.hasAnyAuthority.and.returnValue(false)
    accountService.hasAnyAuthority.withArgs(['ROLE_USER']).and.returnValue(true)
    accountService.isOrganizationOwner.and.returnValue(false)
    accountService.isManageApiCredentialsEnabled.and.returnValue(true)
    accountService.isMFAEnabled.and.returnValue(false)
    accountService.getImageUrl.and.returnValue(null)
    accountService.getMemberId.and.returnValue('123')
    accountService.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['ROLE_USER'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        memberId: '1234',
        loggedAs: false,
        loginAs: '1234',
        mainContact: false,
        mfaEnabled: false,
        manageApiCredsEnabled: true,
      })
    )
    memberService.find.and.returnValue(of({ id: 'id', client_id: 'a', isConsortiumLead: false }))
    createComponent()
    fixture.detectChanges()
    tick()

    const manageApiCredentialsLink = fixture.debugElement.query(By.css('#manageApiCredentialsLink'))
    manageApiCredentialsLink.nativeElement.click()

    expect(modalService.open).toHaveBeenCalledWith(ApiCredentialsMfaEnabledDialogComponent, {
      backdrop: 'static',
      centered: true,
    })
  }))
})
