import { ComponentFixture, TestBed } from '@angular/core/testing'

import { HomeComponent } from './home.component'
import { MemberService } from '../member/service/member.service'
import { AccountService } from '../account'
import { BehaviorSubject, of } from 'rxjs'
import { ISFMemberData, SFMemberData } from '../member/model/salesforce-member-data.model'

describe('HomeComponent', () => {
  let component: HomeComponent
  let fixture: ComponentFixture<HomeComponent>
  let memberServiceSpy: jasmine.SpyObj<MemberService>
  let accountServiceSpy: jasmine.SpyObj<AccountService>

  beforeEach(() => {
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['fetchMemberData'])

    TestBed.configureTestingModule({
      declarations: [HomeComponent],
      providers: [
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
      ],
    })
    fixture = TestBed.createComponent(HomeComponent)
    component = fixture.componentInstance

    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    memberServiceSpy = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
  })

  it('should call get account data', () => {
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

    memberServiceSpy.memberData = new BehaviorSubject<ISFMemberData | undefined | null>(new SFMemberData())

    expect(component).toBeTruthy()

    component.ngOnInit()

    expect(accountServiceSpy.getAccountData).toHaveBeenCalled()
  })

  it('should call fetchMember data if account data is not null', () => {
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

    memberServiceSpy.memberData = new BehaviorSubject<ISFMemberData | undefined | null>(new SFMemberData())

    expect(component).toBeTruthy()

    component.ngOnInit()

    expect(accountServiceSpy.getAccountData).toHaveBeenCalled()
    expect(memberServiceSpy.fetchMemberData).toHaveBeenCalled()
  })
})
