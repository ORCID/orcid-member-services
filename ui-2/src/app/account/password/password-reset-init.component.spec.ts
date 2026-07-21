import { ComponentFixture, TestBed, inject } from '@angular/core/testing'
import { of } from 'rxjs'

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { ReactiveFormsModule } from '@angular/forms'
import { By } from '@angular/platform-browser'
import { PasswordResetInitResult } from '../model/password-reset-init-result.model'
import { PasswordService } from '../service/password.service'
import { PasswordResetInitComponent } from './password-reset-init.component'

describe('Component Tests', () => {
  describe('PasswordResetInitComponent', () => {
    let fixture: ComponentFixture<PasswordResetInitComponent>
    let comp: PasswordResetInitComponent
    beforeEach(() => {
      fixture = TestBed.configureTestingModule({
        imports: [ReactiveFormsModule, PasswordResetInitComponent],
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
      }).createComponent(PasswordResetInitComponent)
      comp = fixture.componentInstance
    })

    it('should define its initial state', () => {
      expect((comp as any).success()).toBeUndefined()
      expect((comp as any).error()).toBeUndefined()
      expect((comp as any).errorEmailNotExists()).toBeUndefined()
    })

    it('notifies of success upon successful requestReset', inject([PasswordService], (service: PasswordService) => {
      spyOn(service, 'initPasswordReset').and.returnValue(of(new PasswordResetInitResult(true, false, false)))
      comp.resetRequestForm.patchValue({
        email: 'user@domain.com',
      })

      comp.requestReset()
      const emailControl = comp.resetRequestForm.get('email')!
      emailControl.setValue('valid@email.com')
      fixture.detectChanges()
      expect((comp as any).success()).toEqual('OK')
      expect((comp as any).error()).toBeUndefined()
      expect((comp as any).errorEmailNotExists()).toBeUndefined()
    }))

    it('shows updated success message with submitted email', inject([PasswordService], (service: PasswordService) => {
      spyOn(service, 'initPasswordReset').and.returnValue(of(new PasswordResetInitResult(true, false, false)))
      comp.resetRequestForm.patchValue({
        email: 'valid@email.com',
      })

      comp.requestReset()
      fixture.detectChanges()

      const successAlert = fixture.debugElement.query(By.css('.alert-success'))
      expect(successAlert).toBeTruthy()

      const successText = successAlert.nativeElement.textContent
      expect(successText).toContain('We have sent a password reset email to')
      expect(successText).toContain('If you do not receive the email, please check your spam folder')
      expect(successText).toContain('Member Portal account.')

      const boldEmail = successAlert.nativeElement.querySelector('strong')
      expect(boldEmail).toBeTruthy()
      expect(boldEmail.textContent.trim()).toBe('valid@email.com')
    }))

    it('notifies of unknown email upon email address not registered/400', inject(
      [PasswordService],
      (service: PasswordService) => {
        spyOn(service, 'initPasswordReset').and.returnValue(of(new PasswordResetInitResult(false, true, false)))
        comp.resetRequestForm.patchValue({
          email: 'user@domain.com',
        })
        comp.requestReset()

        expect(service.initPasswordReset).toHaveBeenCalledWith('user@domain.com')
        expect((comp as any).success()).toBeUndefined()
        expect((comp as any).error()).toBeUndefined()
        expect((comp as any).errorEmailNotExists()).toEqual('ERROR')
      }
    ))

    it('notifies of error upon error response', inject([PasswordService], (service: PasswordService) => {
      spyOn(service, 'initPasswordReset').and.returnValue(of(new PasswordResetInitResult(false, false, true)))
      comp.resetRequestForm.patchValue({
        email: 'user@domain.com',
      })
      comp.requestReset()

      expect(service.initPasswordReset).toHaveBeenCalledWith('user@domain.com')
      expect((comp as any).success()).toBeUndefined()
      expect((comp as any).errorEmailNotExists()).toBeUndefined()
      expect((comp as any).error()).toEqual('ERROR')
    }))

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

    it('should not show validation message while typing before blur/touch', () => {
      const emailControl = comp.resetRequestForm.get('email')!
      emailControl.setValue('invalid-email')
      fixture.detectChanges()

      const errorMessage = fixture.debugElement.query(By.css('small'))
      expect(errorMessage).toBeNull()
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
