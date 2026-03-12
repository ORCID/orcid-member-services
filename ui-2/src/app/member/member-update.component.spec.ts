import { ComponentFixture, TestBed } from '@angular/core/testing'

import { MemberUpdateComponent } from './member-update.component'
import { MemberService } from './service/member.service'
import { RouterTestingModule } from '@angular/router/testing'
import { IMember } from './model/member.model'
import { ActivatedRoute, Router } from '@angular/router'
import { of } from 'rxjs'
import { AlertService } from '../shared/service/alert.service'
import { AlertMessage, AlertType } from '../app.constants'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'

describe('MemberUpdateComponent', () => {
  let component: MemberUpdateComponent
  let fixture: ComponentFixture<MemberUpdateComponent>
  let memberService: jasmine.SpyObj<MemberService>
  let alertService: jasmine.SpyObj<AlertService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>
  let router: jasmine.SpyObj<Router>

  beforeEach(() => {
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['validate', 'update', 'create'])
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])

    TestBed.configureTestingModule({
      declarations: [MemberUpdateComponent],
      imports: [RouterTestingModule.withRoutes([])],
      providers: [
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>

    fixture = TestBed.createComponent(MemberUpdateComponent)
    component = fixture.componentInstance
    memberService.validate.and.returnValue(of({ valid: true }))
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true))
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should update an existing member', () => {
    activatedRoute.data = of({ member: { salesforceId: 'test', id: 'id' } as IMember })
    memberService.update.and.returnValue(of({ id: 'id', salesforceId: 'test' } as IMember))
    fixture.detectChanges()
    component.save()
    expect(memberService.update).toHaveBeenCalled()
    expect(memberService.create).toHaveBeenCalledTimes(0)
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.MEMBER_UPDATED)
    expect(router.navigate).toHaveBeenCalledWith(['/members'])
  })

  it('should create a new member', () => {
    activatedRoute.data = of({})
    memberService.create.and.returnValue(of({ id: 'test' } as IMember))
    component.save()
    expect(memberService.create).toHaveBeenCalled()
    expect(memberService.update).toHaveBeenCalledTimes(0)
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.MEMBER_CREATED)
    expect(router.navigate).toHaveBeenCalledWith(['/members'])
  })

  it('test invalid client id validator', () => {
    component.editForm.get('clientId')?.setValue('test')
    expect(component.editForm.get('clientId')?.valid).toBeFalsy()
  })

  it('test valid client id validator', () => {
    component.editForm.get('clientId')?.setValue('APP-0000000000000000')
    expect(component.editForm.get('clientId')?.valid).toBeTruthy()
  })

  it('test consortium lead parent sfid validator', () => {
    component.editForm.get('isConsortiumLead')?.setValue(true)
    component.editForm.get('salesforceId')?.setValue('sfid')
    component.editForm.get('parentSalesforceId')?.setValue('sfid2')
    expect(component.editForm.get('parentSalesforceId')?.errors).toBeInstanceOf(Object)
  })

  it('test consortium lead parent sfid validator', () => {
    component.editForm.get('isConsortiumLead')?.setValue(true)
    component.editForm.get('salesforceId')?.setValue('sfid')
    component.editForm.get('parentSalesforceId')?.setValue('sfid')

    expect(component.editForm.get('parentSalesforceId')?.errors).toBeNull()
  })

  it('test consortium lead parent sfid validator', () => {
    component.editForm.get('salesforceId')?.setValue('sfid')
    component.editForm.get('parentSalesforceId')?.setValue('sfid2')
    component.editForm.get('isConsortiumLead')?.setValue(false)
    expect(component.editForm.get('parentSalesforceId')?.errors).toBeNull()
  })
})
