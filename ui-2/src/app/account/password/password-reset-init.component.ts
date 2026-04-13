import { AfterViewInit, Component, OnDestroy, Renderer2 } from '@angular/core'
import { FormBuilder, Validators } from '@angular/forms'

import { PasswordResetInitResult } from '../model/password-reset-init-result.model'
import { PasswordService } from '../service/password.service'

@Component({
    selector: 'app-password-reset-init',
    templateUrl: './password-reset-init.component.html',
    standalone: false
})
export class PasswordResetInitComponent implements AfterViewInit, OnDestroy {
  error: string | undefined
  errorEmailNotExists: string | undefined
  success: string | undefined
  resetRequestForm = this.fb.group({
    email: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100), Validators.email]],
  })

  constructor(
    private passwordResetInitService: PasswordService,
    private renderer: Renderer2,
    private fb: FormBuilder
  ) {}

  private focusTimeout: ReturnType<typeof setTimeout> | null = null

  ngAfterViewInit() {
    this.focusTimeout = setTimeout(() => this.renderer.selectRootElement('#email').focus())
  }

  ngOnDestroy() {
    if (this.focusTimeout !== null) {
      clearTimeout(this.focusTimeout)
    }
  }

  requestReset() {
    this.error = undefined
    this.errorEmailNotExists = undefined

    if (this.resetRequestForm.get(['email'])) {
      this.passwordResetInitService
        .initPasswordReset(this.resetRequestForm.get(['email'])!.value)
        .subscribe((result: PasswordResetInitResult | null) => {
          if (result && result.success) {
            this.success = 'OK'
          } else {
            this.success = undefined
            if (result && result.emailNotFound) {
              this.errorEmailNotExists = 'ERROR'
            } else {
              this.error = 'ERROR'
            }
          }
        })
    }
  }
}
