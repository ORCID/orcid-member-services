/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing'

import { CUSTOM_ELEMENTS_SCHEMA, WritableSignal } from '@angular/core'
import { ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute } from '@angular/router'
import { RouterModule } from '@angular/router'
import { QuillModule } from 'ngx-quill'
import { of } from 'rxjs'
import { AccountService } from 'src/app/account'
import { IAccount } from 'src/app/account/model/account.model'
import { SFAddress } from 'src/app/member/model/salesforce-address.model'
import { SFCountry } from 'src/app/member/model/salesforce-country.model'
import { SFMemberContact } from 'src/app/member/model/salesforce-member-contact.model'
import { ISFMemberData, SFConsortiumMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { MemberService } from 'src/app/member/service/member.service'
import { MemberInfoEditComponent } from './member-info-edit.component'

type MemberInfoEditInternals = {
  memberData: WritableSignal<ISFMemberData | undefined | null>
}

const internals = (component: MemberInfoEditComponent): MemberInfoEditInternals =>
  component as unknown as MemberInfoEditInternals

describe('MemberInfoEditComponent', () => {
  let component: MemberInfoEditComponent
  let fixture: ComponentFixture<MemberInfoEditComponent>
  let accountService: jasmine.SpyObj<AccountService>
  let memberService: jasmine.SpyObj<MemberService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  beforeEach(() => {
    spyOn(console, 'error').and.stub()

    const accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    const memberServiceSpy = jasmine.createSpyObj('MemberService', [
      'getMemberData',
      'setManagedMember',
      'getCountries',
      'updateMemberDetails',
      'setMemberData',
    ])
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, RouterModule.forRoot([]), QuillModule.forRoot(), MemberInfoEditComponent],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    memberService.getCountries.and.returnValue(of([new SFCountry('United Kingdom', 'GBR')]))
    memberService.getMemberData.and.returnValue(of(null))
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
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
    fixture.detectChanges()
    expect(memberService.setManagedMember).toHaveBeenCalledOnceWith('test')
    expect(memberService.getMemberData).toHaveBeenCalledOnceWith('test')
  })

  it('should fetch own member`s data', () => {
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
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

    const websiteInput = fixture.nativeElement.querySelector('input[name="website"]') as HTMLInputElement
    expect(websiteInput.value).toEqual('website.com')

    const renderedOrgIds = Array.from(fixture.nativeElement.querySelectorAll('li.contact')).map((row) => ({
      id: (row as HTMLElement).querySelector('.w-66')?.textContent?.trim(),
      name: (row as HTMLElement).querySelector('.w-33')?.textContent?.trim(),
    }))
    expect(renderedOrgIds).toEqual([
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
    accountService.getAccountData.and.returnValue(of({ memberId: 'test' } as IAccount))
    memberService.getMemberData.and.returnValue(of({}))
    fixture.detectChanges()

    const websiteInput = fixture.nativeElement.querySelector('input[name="website"]') as HTMLInputElement

    expect(websiteInput.value).toBe('')

    component.validateUrl()
    expect(websiteInput.value).toBe('')

    websiteInput.value = 'example'
    websiteInput.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    component.validateUrl()
    fixture.detectChanges()
    expect(websiteInput.value).toEqual('http://example')

    websiteInput.value = 'example.com'
    websiteInput.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    component.validateUrl()
    fixture.detectChanges()
    expect(websiteInput.value).toEqual('http://example.com')

    websiteInput.value = 'https://example.com'
    websiteInput.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    component.validateUrl()
    fixture.detectChanges()
    expect(websiteInput.value).toEqual('https://example.com')
  })

  it('should update member data', () => {
    activatedRoute.params = of({ id: 'test' })
    accountService.getAccountData.and.returnValue(of({ memberId: 'test2' } as IAccount))
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
        memberId: 'test2',
      })
    )
    fixture.detectChanges()
    component.editForm.patchValue({ country: 'United Kingdom', website: 'http://website.com' })
    component.save()
    expect(memberService.updateMemberDetails).toHaveBeenCalled()
  })
})
