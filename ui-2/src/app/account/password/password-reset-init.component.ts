import { AfterViewInit, ChangeDetectionStrategy, Component, OnDestroy, Renderer2, inject, signal } from '@angular/core'
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms'

import { PasswordResetInitResult } from '../model/password-reset-init-result.model'
import { PasswordService } from '../service/password.service'

@Component({
  selector: 'app-password-reset-init',
  templateUrl: './password-reset-init.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
})
export class PasswordResetInitComponent implements AfterViewInit, OnDestroy {
  private passwordResetInitService = inject(PasswordService)
  private renderer = inject(Renderer2)
  private fb = inject(FormBuilder)

  protected error = signal<string | undefined>(undefined)
  protected errorEmailNotExists = signal<string | undefined>(undefined)
  protected success = signal<string | undefined>(undefined)
  resetRequestForm = this.fb.group({
    email: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100), Validators.email]],
  })

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
    this.error.set(undefined)
    this.errorEmailNotExists.set(undefined)

    if (this.resetRequestForm.get(['email'])) {
      this.passwordResetInitService
        .initPasswordReset(this.resetRequestForm.get(['email'])!.value)
        .subscribe((result: PasswordResetInitResult | null) => {
          if (result && result.success) {
            this.success.set('OK')
          } else {
            this.success.set(undefined)
            if (result && result.emailNotFound) {
              this.errorEmailNotExists.set('ERROR')
            } else {
              this.error.set('ERROR')
            }
          }
        })
    }
  }
}
