import { ComponentFixture, TestBed } from '@angular/core/testing'

import { RemoveConsortiumMemberComponent } from './remove-consortium-member.component'
import { ActivatedRoute, Router } from '@angular/router'
import { AccountService } from 'src/app/account'
import { MemberService } from 'src/app/member/service/member.service'
import { AlertService } from 'src/app/shared/service/alert.service'
import { RouterTestingModule } from '@angular/router/testing'
import { HttpClientTestingModule } from '@angular/common/http/testing'
import { of } from 'rxjs'
import { AlertType } from 'src/app/app.constants'

describe('RemoveConsortiumMemberComponent', () => {
  let component: RemoveConsortiumMemberComponent
  let fixture: ComponentFixture<RemoveConsortiumMemberComponent>

  let memberServiceSpy: jasmine.SpyObj<MemberService>
  let accountServiceSpy: jasmine.SpyObj<AccountService>
  let alertServiceSpy: jasmine.SpyObj<AlertService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>
  let router: jasmine.SpyObj<Router>

  beforeEach(() => {
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['removeConsortiumMember'])
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      declarations: [RemoveConsortiumMemberComponent],
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

    fixture = TestBed.createComponent(RemoveConsortiumMemberComponent)
    component = fixture.componentInstance

    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true))

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

    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call memberService.removeConsortiumMembers when saving', () => {
    memberServiceSpy.removeConsortiumMember.and.returnValue(of(true))

    // set form to valid
    for (const control in component.editForm.controls) {
      component.editForm.controls[control].clearAsyncValidators()
      component.editForm.controls[control].clearValidators()
      component.editForm.controls[control].updateValueAndValidity({ onlySelf: true })
    }
    component.editForm.updateValueAndValidity()

    component.save()

    expect(memberServiceSpy.removeConsortiumMember).toHaveBeenCalled()
  })

  it('should not call memberService.removeConsortiumMembers when saving if form is invalid', () => {
    // form will be invalid by default

    component.save()
    expect(memberServiceSpy.removeConsortiumMember).toHaveBeenCalledTimes(0)
  })

  it('alert service and router should be called on save success', () => {
    component.onSaveSuccess('orgName')

    expect(alertServiceSpy.broadcast).toHaveBeenCalledWith(AlertType.CONSORTIUM_MEMBER_REMOVED, 'orgName')
    expect(router.navigate).toHaveBeenCalled()
  })
})
