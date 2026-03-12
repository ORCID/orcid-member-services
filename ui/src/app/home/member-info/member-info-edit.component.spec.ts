import { ComponentFixture, TestBed } from '@angular/core/testing'

import { MemberInfoEditComponent } from './member-info-edit.component'
import { AccountService } from 'src/app/account'
import { MemberService } from 'src/app/member/service/member.service'
import { ActivatedRoute } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { of } from 'rxjs'
import { IAccount } from 'src/app/account/model/account.model'
import { SFAddress } from 'src/app/member/model/salesforce-address.model'
import { SFMemberContact } from 'src/app/member/model/salesforce-member-contact.model'
import { SFConsortiumMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { SFCountry } from 'src/app/member/model/salesforce-country.model'

describe('MemberInfoEditComponent', () => {
  let component: MemberInfoEditComponent
  let fixture: ComponentFixture<MemberInfoEditComponent>
  let accountService: jasmine.SpyObj<AccountService>
  let memberService: jasmine.SpyObj<MemberService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  beforeEach(() => {
    const accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    const memberServiceSpy = jasmine.createSpyObj('MemberService', [
      'getMemberData',
      'setManagedMember',
      'getCountries',
      'updateMemberDetails',
      'setMemberData',
    ])
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes([])],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
      ],
      declarations: [MemberInfoEditComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    memberService.getCountries.and.returnValue(of([new SFCountry('United Kingdom', 'GBR')]))
    fixture = TestBed.createComponent(MemberInfoEditComponent)
    component = fixture.componentInstance
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should not fetch member data without an account', () => {
    accountService.getAccountData.and.returnValue(of(undefined))
    fixture.detectChanges()
    expect(memberService.setManagedMember).toHaveBeenCalledTimes(0)
    expect(memberService.getMemberData).toHaveBeenCalledTimes(0)
  })

  it('should fetch managed member`s data', () => {
    activatedRoute.params = of({ id: 'test' })
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
    fixture.detectChanges()
    expect(memberService.setManagedMember).toHaveBeenCalledOnceWith('test')
    expect(memberService.getMemberData).toHaveBeenCalledOnceWith('test')
  })

  it('should fetch own member`s data', () => {
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
    memberService.getMemberData.and.returnValue(
      of({
        id: 'SfId',
        consortiaMember: true,
        consortiaLeadId: 'SfId',
        isConsortiumLead: true,
        name: 'Member name',
        publicDisplayName: 'Public display name',
        website: 'website.com',
        billingCountry: 'Lithuania',
        memberType: 'memberType',
        publicDisplayDescriptionHtml: 'Public dispay description',
        logoUrl: 'https://orcid.org/assets/vectors/orcid.logo.icon.svg',
        publicDisplayEmail: 'hehe@mail.com',
        membershipStartDateString: '2022',
        membershipEndDateString: '2050',
        consortiumLeadName: '',
        consortiumMembers: [
          new SFConsortiumMemberData('sfid1', 'orgname1'),
          new SFConsortiumMemberData('sfid2', 'orgname 2'),
        ],
        contacts: [
          new SFMemberContact('contactId1', true, ['Invoice contact'], 'Anthony', 'email2@email.com', 'title', 'phone'),
          new SFMemberContact(
            'contactId2',
            false,
            ['Voting contact'],
            'Barbara',
            'email@email.com',
            'other title',
            'phone'
          ),
        ],
        orgIds: { ROR: ['123', '456'], GRID: ['1213', '1415'] },
        billingAddress: new SFAddress('city', 'United Kingdom', 'GBR', 'postalCode', 'England', 'stateCode', 'street'),
      })
    )
    fixture.detectChanges()
    expect(memberService.setManagedMember).toHaveBeenCalledTimes(0)
    expect(memberService.getMemberData).toHaveBeenCalledOnceWith('test2')
    expect(component.memberData?.website).toEqual('http://website.com')
    expect(component.orgIdsTransformed).toEqual([
      { id: '123', name: 'ROR' },
      { id: '456', name: 'ROR' },
      { id: '1213', name: 'GRID' },
      { id: '1415', name: 'GRID' },
    ])
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

  it('should update member data', () => {
    activatedRoute.params = of({ id: 'test' })
    accountService.getAccountData.and.returnValue(of({ salesforceId: 'test2' } as IAccount))
    memberService.updateMemberDetails.and.returnValue(of({}))
    memberService.getMemberData.and.returnValue(
      of({
        id: 'SfId',
        consortiaMember: true,
        consortiaLeadId: 'SfId',
        isConsortiumLead: true,
        name: 'Member name',
        publicDisplayName: 'Public display name',
        website: 'website.com',
        billingCountry: 'Lithuania',
        memberType: 'memberType',
        publicDisplayDescriptionHtml: 'Public dispay description',
        logoUrl: 'https://orcid.org/assets/vectors/orcid.logo.icon.svg',
        publicDisplayEmail: 'hehe@mail.com',
        membershipStartDateString: '2022',
        membershipEndDateString: '2050',
        consortiumLeadName: '',
        consortiumMembers: [
          new SFConsortiumMemberData('sfid1', 'orgname1'),
          new SFConsortiumMemberData('sfid2', 'orgname 2'),
        ],
        contacts: [
          new SFMemberContact('contactId1', true, ['Invoice contact'], 'Anthony', 'email2@email.com', 'title', 'phone'),
          new SFMemberContact(
            'contactId2',
            false,
            ['Voting contact'],
            'Barbara',
            'email@email.com',
            'other title',
            'phone'
          ),
        ],
        orgIds: { ROR: ['123', '456'], GRID: ['1213', '1415'] },
        billingAddress: new SFAddress('city', 'United Kingdom', 'GBR', 'postalCode', 'England', 'stateCode', 'street'),
      })
    )
    fixture.detectChanges()
    component.save()
    expect(memberService.updateMemberDetails).toHaveBeenCalled()
  })
})
