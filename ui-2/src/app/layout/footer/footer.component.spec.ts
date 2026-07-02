import { ComponentFixture, TestBed } from '@angular/core/testing'

import { By } from '@angular/platform-browser'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { BehaviorSubject, of } from 'rxjs'
import { AccountService } from 'src/app/account/service/account.service'
import { FooterComponent } from './footer.component'

describe('FooterComponent', () => {
  let component: FooterComponent
  let fixture: ComponentFixture<FooterComponent>
  let accountService: jasmine.SpyObj<AccountService>
  let oidcAuthState: BehaviorSubject<{ isAuthenticated: boolean }>

  beforeEach(() => {
    oidcAuthState = new BehaviorSubject<{ isAuthenticated: boolean }>({ isAuthenticated: false })
    const accountServiceSpy = jasmine.createSpyObj('AccountService', ['getReleaseVersion'])
    const oidcSecurityServiceMock = {
      isAuthenticated$: oidcAuthState.asObservable(),
    }

    TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: OidcSecurityService, useValue: oidcSecurityServiceMock },
      ],
    }).compileComponents()

    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    accountService.getReleaseVersion.and.returnValue(of(null))

    fixture = TestBed.createComponent(FooterComponent)
    component = fixture.componentInstance
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should contain copyright notice', () => {
    fixture.detectChanges()
    const copyright = fixture.debugElement.query(By.css('.copyright'))
    expect(copyright).toBeTruthy()
    expect(copyright.nativeElement.textContent).toContain(new Date().getFullYear().toString())
  })

  it('should expose an accessible label on the ORCID home link', () => {
    fixture.detectChanges()
    const logoLink = fixture.debugElement.query(By.css('.copyright a'))
    const logoImage = fixture.debugElement.query(By.css('.copyright img'))
    expect(logoLink.attributes['aria-label']).toBe('ORCID')
    expect(logoImage.attributes['alt']).toBe('')
  })

  it('if not authenticated, should not contain help link', () => {
    oidcAuthState.next({ isAuthenticated: false })
    fixture.detectChanges()
    const helpLink = fixture.debugElement.query(By.css('#helpLink'))
    expect(helpLink).toBeFalsy()
  })

  it('if authenticated, should contain help link', () => {
    oidcAuthState.next({ isAuthenticated: true })
    fixture.detectChanges()
    const helpLink = fixture.debugElement.query(By.css('#helpLink'))
    expect(helpLink).toBeTruthy()
  })
})
