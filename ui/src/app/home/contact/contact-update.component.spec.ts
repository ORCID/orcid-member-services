import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ContactUpdateComponent } from './contact-update.component'
import { AppModule } from '../../app.module'
import { MemberService } from 'src/app/member/service/member.service'
import { AccountService } from 'src/app/account'
import { HttpClientTestingModule } from '@angular/common/http/testing'
import { ActivatedRoute, Router } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { of } from 'rxjs'
import { SFMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { AlertService } from 'src/app/shared/service/alert.service'
import { AlertType } from 'src/app/app.constants'

describe('ContactUpdateComponent', () => {
  let component: ContactUpdateComponent
  let fixture: ComponentFixture<ContactUpdateComponent>
  let memberServiceSpy: jasmine.SpyObj<MemberService>
  let accountServiceSpy: jasmine.SpyObj<AccountService>
  let alertServiceSpy: jasmine.SpyObj<AlertService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>
  let router: jasmine.SpyObj<Router>

  beforeEach(() => {
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['find', 'getMemberData', 'updateContact'])
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      declarations: [ContactUpdateComponent],
      providers: [
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
      ],
    })
    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    memberServiceSpy = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    alertServiceSpy = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>

    fixture = TestBed.createComponent(ContactUpdateComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true))
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call getAccountData and getMemberData to get contact data on init', () => {
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
        mfaEnabled: true,
      })
    )

    memberServiceSpy.getMemberData.and.returnValue(
      of({
        id: 'some-id',
        contacts: [
          {
            memberId: 'some-id',
            votingContant: false,
            memberOrgRole: ['role'],
            name: 'contact 1',
            contactEmail: 'contact1@orcid.org',
            title: 'title',
            phone: '0123456789',
          },
        ],
      })
    )

    component.contactId = 'contact1@orcid.org'

    component.ngOnInit()

    expect(accountServiceSpy.getAccountData).toHaveBeenCalled()
    expect(memberServiceSpy.getMemberData).toHaveBeenCalled()
    expect(component.contact).toBeTruthy()
  })

  it('should call memberService.updateContact when saving', () => {
    component.memberData = { name: 'member' }
    component.contact = {
      memberId: 'some-id',
      votingContant: false,
      memberOrgRole: ['role'],
      name: 'contact 1',
      contactEmail: 'contact1@orcid.org',
      title: 'title',
      phone: '0123456789',
    }

    memberServiceSpy.updateContact.and.returnValue(of(true))

    // set form to valid
    for (const control in component.editForm.controls) {
      component.editForm.controls[control].clearAsyncValidators()
      component.editForm.controls[control].clearValidators()
      component.editForm.controls[control].updateValueAndValidity({ onlySelf: true })
    }
    component.editForm.updateValueAndValidity()

    component.contactId = 'contact1@orcid.org'

    component.save()

    expect(memberServiceSpy.updateContact).toHaveBeenCalled()
  })

  it('should not call memberService.updateContact when saving if form is invalid', () => {
    // form is invalid as we haven't set its values
    component.save()
    expect(memberServiceSpy.updateContact).toHaveBeenCalledTimes(0)
  })

  it('should call memberService.updateContact when deleting', () => {
    component.memberData = { name: 'member' }
    component.contact = {
      memberId: 'some-id',
      votingContant: false,
      memberOrgRole: ['role'],
      name: 'contact 1',
      contactEmail: 'contact1@orcid.org',
      title: 'title',
      phone: '0123456789',
    }

    memberServiceSpy.updateContact.and.returnValue(of(true))

    // set form to valid
    for (const control in component.editForm.controls) {
      component.editForm.controls[control].clearAsyncValidators()
      component.editForm.controls[control].clearValidators()
      component.editForm.controls[control].updateValueAndValidity({ onlySelf: true })
    }
    component.editForm.updateValueAndValidity()

    component.contactId = 'contact1@orcid.org'

    component.delete()

    expect(memberServiceSpy.updateContact).toHaveBeenCalled()
  })

  it('alert service and router should be called on save success', () => {
    component.onSaveSuccess()
    expect(alertServiceSpy.broadcast).toHaveBeenCalledWith(AlertType.CONTACT_UPDATED)
  })
})
