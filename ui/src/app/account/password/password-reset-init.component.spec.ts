import { ComponentFixture, TestBed, inject } from '@angular/core/testing'
import { Renderer2, ElementRef } from '@angular/core'
import { FormBuilder } from '@angular/forms'
import { Observable, of, throwError } from 'rxjs'

import { PasswordResetInitService } from '../service/password-reset-init.service'
import { PasswordResetInitComponent } from './password-reset-init.component'
import { EMAIL_NOT_FOUND_TYPE } from 'src/app/app.constants'
import { HttpClientTestingModule } from '@angular/common/http/testing'

describe('Component Tests', () => {
  describe('PasswordResetInitComponent', () => {
    let fixture: ComponentFixture<PasswordResetInitComponent>
    let comp: PasswordResetInitComponent

    beforeEach(() => {
      fixture = TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        declarations: [PasswordResetInitComponent],
        providers: [
          FormBuilder,
          {
            provide: Renderer2,
            useValue: {
              invokeElementMethod(renderElement: any, methodName: string, args?: any[]) {},
            },
          },
          {
            provide: ElementRef,
            useValue: new ElementRef(null),
          },
        ],
      })
        .overrideTemplate(PasswordResetInitComponent, '')
        .createComponent(PasswordResetInitComponent)
      comp = fixture.componentInstance
    })

    it('should define its initial state', () => {
      expect(comp.success).toBeUndefined()
      expect(comp.error).toBeUndefined()
      expect(comp.errorEmailNotExists).toBeUndefined()
    })

    it('sets focus after the view has been initialized', () => {
      const elementRefSpy = jasmine.createSpyObj('ElementRef', ['nativeElement'])
      const nativeElement = document.createElement('div')
      elementRefSpy.nativeElement = nativeElement

      spyOn(nativeElement, 'scrollIntoView')

      comp.ngAfterViewInit()

      expect(nativeElement.scrollIntoView).toHaveBeenCalled()
    })

    it('notifies of success upon successful requestReset', inject(
      [PasswordResetInitService],
      (service: PasswordResetInitService) => {
        spyOn(service, 'initPasswordReset').and.returnValue(of({}))
        comp.resetRequestForm.patchValue({
          email: 'user@domain.com',
        })

        comp.requestReset()

        expect(service.initPasswordReset).toHaveBeenCalledWith('user@domain.com')
        expect(comp.success).toEqual('OK')
        expect(comp.error).toBeUndefined()
        expect(comp.errorEmailNotExists).toBeUndefined()
      }
    ))

    it('notifies of unknown email upon email address not registered/400', inject(
      [PasswordResetInitService],
      (service: PasswordResetInitService) => {
        spyOn(service, 'initPasswordReset').and.returnValue(
          throwError({
            status: 400,
            error: { type: EMAIL_NOT_FOUND_TYPE },
          })
        )
        comp.resetRequestForm.patchValue({
          email: 'user@domain.com',
        })
        comp.requestReset()

        expect(service.initPasswordReset).toHaveBeenCalledWith('user@domain.com')
        expect(comp.success).toBeUndefined()
        expect(comp.error).toBeUndefined()
        expect(comp.errorEmailNotExists).toEqual('ERROR')
      }
    ))

    it('notifies of error upon error response', inject(
      [PasswordResetInitService],
      (service: PasswordResetInitService) => {
        spyOn(service, 'initPasswordReset').and.returnValue(
          throwError({
            status: 503,
            data: 'something else',
          })
        )
        comp.resetRequestForm.patchValue({
          email: 'user@domain.com',
        })
        comp.requestReset()

        expect(service.initPasswordReset).toHaveBeenCalledWith('user@domain.com')
        expect(comp.success).toBeUndefined()
        expect(comp.errorEmailNotExists).toBeUndefined()
        expect(comp.error).toEqual('ERROR')
      }
    ))
  })
})
