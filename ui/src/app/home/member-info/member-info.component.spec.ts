import { ComponentFixture, TestBed } from '@angular/core/testing'

import { MemberInfoComponent } from './member-info.component'
import { AccountService } from 'src/app/account'
import { MemberService } from 'src/app/member/service/member.service'
import { RouterTestingModule } from '@angular/router/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { of } from 'rxjs'
import { IAccount } from 'src/app/account/model/account.model'

describe('MemberInfoComponent', () => {
  let component: MemberInfoComponent
  let fixture: ComponentFixture<MemberInfoComponent>
  let accountService: jasmine.SpyObj<AccountService>
  let memberService: jasmine.SpyObj<MemberService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  beforeEach(() => {
    const accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['getMemberData', 'setManagedMember'])
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes([])],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
      ],
      declarations: [MemberInfoComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
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
    expect(component.managedMember).toEqual('test')
    expect(memberService.getMemberData).toHaveBeenCalledTimes(0)
    expect(component.memberData).toBeUndefined()
  })

  it('should call the member service while managing a member', () => {
    activatedRoute.params = of({ id: 'test' })
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
    fixture.detectChanges()

    expect(memberService.setManagedMember).toHaveBeenCalledWith('test')
    expect(memberService.getMemberData).toHaveBeenCalledWith('test')
  })

  it('should call the member service without managing a member', () => {
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
    fixture.detectChanges()

    expect(memberService.setManagedMember).toHaveBeenCalledTimes(0)
    expect(memberService.getMemberData).toHaveBeenCalledWith('test2')
  })

  it('should stop managing member', () => {
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
    fixture.detectChanges()

    component.stopManagingMember()
    expect(memberService.setManagedMember).toHaveBeenCalledWith(null)
    expect(memberService.getMemberData).toHaveBeenCalledWith('test2', true)
  })

  it('member should be active', () => {
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
    memberService.getMemberData.and.returnValue(of({ membershipEndDateString: '2050' }))
    fixture.detectChanges()

    const res = component.isActive()
    expect(res).toEqual(true)
  })

  it('member should be inactive', () => {
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
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
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test' } as IAccount))
    memberService.getMemberData.and.returnValue(of({}))
    fixture.detectChanges()

    expect(component.memberData).toBeDefined
    expect(component.memberData!.website).toBeUndefined()

    component.validateUrl()
    expect(component.memberData!.website).toBeUndefined()

    component.memberData!.website = 'example'
    component.validateUrl()
    expect(component.memberData!.website).toEqual('http://example')

    component.memberData!.website = 'example.com'
    component.validateUrl()
    expect(component.memberData!.website).toEqual('http://example.com')

    component.memberData!.website = 'https://example.com'
    component.validateUrl()
    expect(component.memberData!.website).toEqual('https://example.com')
  })
})
