import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AddConsortiumMemberComponent } from './add-consortium-member.component'
import { MemberService } from 'src/app/member/service/member.service'
import { AccountService } from 'src/app/account'
import { AlertService } from 'src/app/shared/service/alert.service'
import { ActivatedRoute, Router } from '@angular/router'
import { HttpClientTestingModule } from '@angular/common/http/testing'
import { RouterTestingModule } from '@angular/router/testing'
import { of } from 'rxjs'
import { AlertType } from 'src/app/app.constants'

describe('AddConsortiumMemberComponent', () => {
  let component: AddConsortiumMemberComponent
  let fixture: ComponentFixture<AddConsortiumMemberComponent>

  let memberServiceSpy: jasmine.SpyObj<MemberService>
  let accountServiceSpy: jasmine.SpyObj<AccountService>
  let alertServiceSpy: jasmine.SpyObj<AlertService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>
  let router: jasmine.SpyObj<Router>

  beforeEach(() => {
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['addConsortiumMember'])
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      declarations: [AddConsortiumMemberComponent],
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

    fixture = TestBed.createComponent(AddConsortiumMemberComponent)
    component = fixture.componentInstance

    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true))

    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
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

    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should be a valid form when all required fields are filled', () => {
    component.editForm.controls['orgName'].setValue('Orcid')
    component.editForm.controls['startMonth'].setValue('01')
    component.editForm.controls['startYear'].setValue('2025')
    component.editForm.controls['trademarkLicense'].setValue('Yes')
    component.editForm.controls['organizationTier'].setValue('Small')
    fixture.detectChanges()

    expect(component.editForm.valid).toBeTrue()
  })

  it('should be a invalid form when a required field is missing', () => {
    component.editForm.controls['orgName'].setValue('Orcid')
    component.editForm.controls['startMonth'].setValue('01')
    component.editForm.controls['startYear'].setValue('2025')
    component.editForm.controls['trademarkLicense'].setValue('')
    component.editForm.controls['organizationTier'].setValue('Small')
    fixture.detectChanges()

    expect(component.editForm.invalid).toBeTrue()
  })

  it('should call memberService.addConsortiumMember when saving', () => {
    memberServiceSpy.addConsortiumMember.and.returnValue(of(true))

    // set form to valid
    for (const control in component.editForm.controls) {
      component.editForm.controls[control].clearAsyncValidators()
      component.editForm.controls[control].clearValidators()
      component.editForm.controls[control].updateValueAndValidity({ onlySelf: true })
    }
    component.editForm.updateValueAndValidity()

    component.save()

    expect(memberServiceSpy.addConsortiumMember).toHaveBeenCalled()
  })

  it('should not call memberService.addConsortiumMember when saving if form is invalid', () => {
    // form will be invalid by default
    component.save()
    expect(memberServiceSpy.addConsortiumMember).toHaveBeenCalledTimes(0)
  })

  it('alert service and router should be called on save success', () => {
    component.onSaveSuccess('orgName')
    expect(alertServiceSpy.broadcast).toHaveBeenCalledWith(AlertType.CONSORTIUM_MEMBER_ADDED, 'orgName')
    expect(router.navigate).toHaveBeenCalled()
  })
})
