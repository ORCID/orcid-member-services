import { ComponentFixture, TestBed } from '@angular/core/testing'

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { of } from 'rxjs'
import { AlertMessage, AlertType } from '../app.constants'
import { AlertService } from '../shared/service/alert.service'
import { MemberUpdateComponent } from './member-update.component'
import { IMember, Member } from './model/member.model'
import { MemberService } from './service/member.service'

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
      imports: [RouterTestingModule.withRoutes([]), ReactiveFormsModule],
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

  describe('salesforceId validation messages', () => {
      beforeEach(() => {
        activatedRoute.data = of({ member: new Member() })
        fixture.detectChanges()
      })

      it('should show "This field is required" when salesforceId is empty and touched', () => {
        const input = fixture.nativeElement.querySelector('#field_salesforceId')
        input.value = ''
        input.dispatchEvent(new Event('input'))
        component.editForm.get('salesforceId')?.markAsTouched()
        component.editForm.get('salesforceId')?.updateValueAndValidity()
        fixture.detectChanges()

        const requiredMsg = fixture.nativeElement.querySelector('[data-cy="fieldIsRequired"]')
        expect(requiredMsg).toBeTruthy()
        expect(requiredMsg.textContent.trim()).toBe('This field is required.')
      })

      it('should show "Must be exactly 18 alphanumeric characters" when salesforceId format is invalid', () => {
        const input = fixture.nativeElement.querySelector('#field_salesforceId')
        input.value = 'short'
        input.dispatchEvent(new Event('input'))
        component.editForm.get('salesforceId')?.markAsTouched()
        component.editForm.get('salesforceId')?.updateValueAndValidity()
        fixture.detectChanges()

        const patternMsg = fixture.nativeElement.querySelector('[data-cy="fieldPatternInvalid"]')
        expect(patternMsg).toBeTruthy()
        expect(patternMsg.textContent.trim()).toBe('Must be exactly 18 alphanumeric characters.')
      })

      it('should not show any error messages when salesforceId is valid', () => {
        const input = fixture.nativeElement.querySelector('#field_salesforceId')
        input.value = 'ABCDEFGHIJKLMNOPQR'
        input.dispatchEvent(new Event('input'))
        component.editForm.get('salesforceId')?.markAsTouched()
        component.editForm.get('salesforceId')?.updateValueAndValidity()
        fixture.detectChanges()

        expect(fixture.nativeElement.querySelector('[data-cy="fieldIsRequired"]')).toBeNull()
        expect(fixture.nativeElement.querySelector('[data-cy="fieldPatternInvalid"]')).toBeNull()
      })
    })

    it('should create a new member', () => {
    activatedRoute.data = of({ member: new Member() })
    memberService.create.and.returnValue(of({ id: 'test' } as IMember))
    component.save()
    expect(memberService.create).toHaveBeenCalled()
    expect(memberService.update).toHaveBeenCalledTimes(0)
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.MEMBER_CREATED)
    expect(router.navigate).toHaveBeenCalledWith(['/members'])
  })

})
