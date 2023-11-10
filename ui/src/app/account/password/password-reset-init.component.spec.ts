import { ComponentFixture, TestBed, inject } from '@angular/core/testing'
import { Renderer2, ElementRef } from '@angular/core'
import { FormBuilder } from '@angular/forms'
import { of, throwError } from 'rxjs'

import { PasswordResetInitService } from '../service/password-reset-init.service'
import { PasswordResetInitComponent } from './password-reset-init.component'
import { EMAIL_NOT_FOUND_TYPE } from 'src/app/app.constants'
import { HttpClientTestingModule } from '@angular/common/http/testing'
import { By } from '@angular/platform-browser'

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
      }).createComponent(PasswordResetInitComponent)
      comp = fixture.componentInstance
    })

    it('should define its initial state', () => {
      expect(comp.success).toBeUndefined()
      expect(comp.error).toBeUndefined()
      expect(comp.errorEmailNotExists).toBeUndefined()
    })

    it('notifies of success upon successful requestReset', inject(
      [PasswordResetInitService],
      (service: PasswordResetInitService) => {
        spyOn(service, 'initPasswordReset').and.returnValue(of({}))
        comp.resetRequestForm.patchValue({
          email: 'user@domain.com',
        })

        comp.requestReset()
        const emailControl = comp.resetRequestForm.get('email')!
        emailControl.setValue('valid@email.com')
        fixture.detectChanges()
        expect(comp.success).toEqual('OK')
        expect(comp.error).toBeUndefined()
        expect(comp.errorEmailNotExists).toBeUndefined()
        fixture.whenStable().then(() => {
          expect(true).toBeFalsy()
          const button = fixture.debugElement.query(By.css('#reset'))
          expect(button.nativeElement.disabled).toBeFalsy()
        })
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

    it('should disable the submit button for invalid email address', () => {
      const emailControl = comp.resetRequestForm.get('email')!
      emailControl.markAsTouched()
      emailControl.setValue('invalid-email')
      fixture.detectChanges()
      const errorMessage = fixture.debugElement.query(By.css('small'))
      expect(errorMessage).toBeTruthy()
      const errorText = errorMessage.nativeElement.textContent.trim()
      expect(errorText).toBe('Your email is invalid.')
      const button = fixture.debugElement.query(By.css('#reset'))
      expect(button.nativeElement.disabled).toBeTruthy()
    })

    it('should disable the submit button for empty email address field', () => {
      const emailControl = comp.resetRequestForm.get('email')!
      emailControl.markAsTouched()
      fixture.detectChanges()
      const errorMessage = fixture.debugElement.query(By.css('small'))
      expect(errorMessage).toBeTruthy()
      const errorText = errorMessage.nativeElement.textContent.trim()
      expect(errorText).toBe('Your email is required.')
      const button = fixture.debugElement.query(By.css('#reset'))
      expect(button.nativeElement.disabled).toBeTruthy()
    })

    it('should disable the submit button for short email address', () => {
      const emailControl = comp.resetRequestForm.get('email')!
      emailControl.setValue('i@a')
      emailControl.markAsTouched()
      fixture.detectChanges()
      const errorMessage = fixture.debugElement.query(By.css('small'))
      expect(errorMessage).toBeTruthy()
      const errorText = errorMessage.nativeElement.textContent.trim()
      expect(errorText).toBe('Your email is required to be at least 5 characters.')
      const button = fixture.debugElement.query(By.css('#reset'))
      expect(button.nativeElement.disabled).toBeTruthy()
    })

    it('should disable the submit button for long email address', () => {
      const emailControl = comp.resetRequestForm.get('email')!
      emailControl.setValue(
        'abcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcdeabcde@mail.com'
      )
      emailControl.markAsTouched()
      fixture.detectChanges()
      const errorMessage = fixture.debugElement.query(By.css('#maxlengthError'))
      expect(errorMessage).toBeTruthy()
      const errorText = errorMessage.nativeElement.textContent.trim()
      expect(errorText).toBe('Your email cannot be longer than 100 characters.')
      const button = fixture.debugElement.query(By.css('#reset'))
      expect(button.nativeElement.disabled).toBeTruthy()
    })
  })
})
