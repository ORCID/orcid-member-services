/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing'

import { MemberInfoComponent } from './member-info.component'
import { AccountService } from 'src/app/account'
import { MemberService } from 'src/app/member/service/member.service'
import { RouterModule } from '@angular/router'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { of } from 'rxjs'
import { IAccount } from 'src/app/account/model/account.model'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'

describe('MemberInfoComponent', () => {
  let component: MemberInfoComponent
  let fixture: ComponentFixture<MemberInfoComponent>
  let accountService: jasmine.SpyObj<AccountService>
  let memberService: jasmine.SpyObj<MemberService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  beforeEach(() => {
    const accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['getMemberData', 'setManagedMember'])
    const mockOidcSecurityService = {
      checkAuth: () => of({ isAuthenticated: true, userData: { email: 'test@email.com' } }),
      userData$: of({ email: 'test@email.com' }),
      isAuthenticated$: of({ isAuthenticated: true }),
      logoff: jasmine.createSpy('logoff'),
    }
    const mockAccount: IAccount = {
      id: 'test-id',
      activated: true,
      authorities: ['ROLE_USER'],
      email: 'test@email.com',
      firstName: 'Test',
      langKey: 'en',
      lastName: 'User',
      imageUrl: '',
      memberId: 'test2',
      loggedAs: false,
      loginAs: '',
      mainContact: false,
      mfaEnabled: false,
      manageApiCredsEnabled: false,
    }

    accountServiceSpy.getAccountData.and.returnValue(of(mockAccount))
    memberServiceSpy.getMemberData.and.returnValue(of(null))

    TestBed.configureTestingModule({
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [RouterModule.forRoot([]), MemberInfoComponent],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: OidcSecurityService, useValue: mockOidcSecurityService },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    })
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    fixture = TestBed.createComponent(MemberInfoComponent)
    component = fixture.componentInstance
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should not call the member service without a provided account', () => {
    activatedRoute.params = of({ id: 'test' })
    accountService.getAccountData.and.returnValue(of(undefined))
    fixture.detectChanges()

    expect(accountService.getAccountData).toHaveBeenCalled()
    expect(memberService.setManagedMember).toHaveBeenCalledTimes(0)
    expect((component as any).managedMember()).toBeUndefined()
    expect(memberService.getMemberData).toHaveBeenCalledTimes(0)
    expect((component as any).memberData()).toBeNull()
  })

  it('should call the member service while managing a member', () => {
    activatedRoute.params = of({ id: 'test' })
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
    fixture.detectChanges()

    expect(memberService.setManagedMember).toHaveBeenCalledWith('test')
    expect(memberService.getMemberData).toHaveBeenCalledWith('test')
  })

  it('should call the member service without managing a member', () => {
    // Assuming you have mock route params of {} here in the setup somewhere
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
    fixture.detectChanges()

    // Update these two lines:
    expect(memberService.setManagedMember).toHaveBeenCalledTimes(1)
    expect(memberService.setManagedMember).toHaveBeenCalledWith(null)

    expect(memberService.getMemberData).toHaveBeenCalledWith('test2')
  })

  it('should stop managing member', () => {
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
    fixture.detectChanges()

    component.stopManagingMember()
    expect(memberService.setManagedMember).toHaveBeenCalledWith(null)
    expect(memberService.getMemberData).toHaveBeenCalledWith('test2', true)
  })

  it('member should be active', () => {
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
    memberService.getMemberData.and.returnValue(of({ membershipEndDateString: '2050' }))
    fixture.detectChanges()

    const res = component.isActive()
    expect(res).toEqual(true)
  })

  it('member should be inactive', () => {
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
    memberService.getMemberData.and.returnValue(of({ membershipEndDateString: '2022' }))
    fixture.detectChanges()

    const res = component.isActive()
    expect(res).toEqual(false)
  })

  it('test crossref id filter', () => {
    let res = component.filterCRFID('test')
    expect(res).toEqual('test')
    res = component.filterCRFID('http://dx.doi.org/123')
    expect(res).toEqual('123')
    res = component.filterCRFID('https://dx.doi.org/12345')
    expect(res).toEqual('12345')
    res = component.filterCRFID('dx.doi.org/123456.123/123')
    expect(res).toEqual('123456.123/123')
  })

  it('should add protocol to websites where it is missing', () => {
    accountService.getAccountData.and.returnValue(of({ memberId: 'test' } as IAccount))
    memberService.getMemberData.and.returnValue(of({}))
    fixture.detectChanges()

    expect((component as any).memberData()).toBeDefined()
    expect((component as any).memberData()!.website).toBeUndefined()

    component.validateUrl()
    expect((component as any).memberData()!.website).toBeUndefined()
    ;(component as any).memberData.set({ ...(component as any).memberData(), website: 'example' })
    component.validateUrl()
    expect((component as any).memberData()!.website).toEqual('http://example')
    ;(component as any).memberData.set({ ...(component as any).memberData(), website: 'example.com' })
    component.validateUrl()
    expect((component as any).memberData()!.website).toEqual('http://example.com')
    ;(component as any).memberData.set({ ...(component as any).memberData(), website: 'https://example.com' })
    component.validateUrl()
    expect((component as any).memberData()!.website).toEqual('https://example.com')
  })
})
