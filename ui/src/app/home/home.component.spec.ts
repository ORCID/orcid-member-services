import { ComponentFixture, TestBed } from '@angular/core/testing'

import { HomeComponent } from './home.component'
import { AccountService } from '../account'
import { of } from 'rxjs'

describe('HomeComponent', () => {
  let component: HomeComponent
  let fixture: ComponentFixture<HomeComponent>
  let accountServiceSpy: jasmine.SpyObj<AccountService>

  beforeEach(() => {
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])

    TestBed.configureTestingModule({
      declarations: [HomeComponent],
      providers: [{ provide: AccountService, useValue: accountServiceSpy }],
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

    expect(component).toBeTruthy()

    component.ngOnInit()

    expect(accountServiceSpy.getAccountData).toHaveBeenCalled()
    expect(component.loggedInMessage).toEqual('You are logged in as user <strong>email@email.com</strong>')
  })
})
