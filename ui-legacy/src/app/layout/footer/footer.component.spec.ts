import { ComponentFixture, TestBed } from '@angular/core/testing'

import { FooterComponent } from './footer.component'
import { By } from '@angular/platform-browser'
import { AccountService } from 'src/app/account/service/account.service'

describe('FooterComponent', () => {
  let component: FooterComponent
  let fixture: ComponentFixture<FooterComponent>
  let accountService: jasmine.SpyObj<AccountService>

  beforeEach(() => {
    const accountServiceSpy = jasmine.createSpyObj('AccountService', ['isAuthenticated'])

    TestBed.configureTestingModule({
      declarations: [FooterComponent],
      providers: [{ provide: AccountService, useValue: accountServiceSpy }],
    }).compileComponents()

    fixture = TestBed.createComponent(FooterComponent)
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    fixture.detectChanges()
    component = fixture.componentInstance
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should contain copyright notice', () => {
    const copyright = fixture.debugElement.query(By.css('.copyright'))
    expect(copyright).toBeTruthy()
  })

  it('if not authenticated, should not contain help link', () => {
    accountService.isAuthenticated.and.returnValue(false)
    fixture.detectChanges()
    const helpLink = fixture.debugElement.query(By.css('#helpLink'))
    expect(helpLink).toBeFalsy()
  })

  it('if authenticated, should contain help link', () => {
    accountService.isAuthenticated.and.returnValue(true)
    fixture.detectChanges()
    const helpLink = fixture.debugElement.query(By.css('#helpLink'))
    expect(helpLink).toBeTruthy()
  })
})
