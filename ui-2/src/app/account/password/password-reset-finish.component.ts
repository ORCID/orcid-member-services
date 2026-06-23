import { PasswordService } from '../service/password.service'
import { ChangeDetectionStrategy, Component, OnInit, AfterViewInit, Renderer2, ElementRef, inject, signal } from '@angular/core'
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { PasswordStrengthComponent } from './password-strength.component'

@Component({
  selector: 'app-password-reset-finish',
  templateUrl: './password-reset-finish.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, PasswordStrengthComponent],
})
export class PasswordResetFinishComponent implements OnInit, AfterViewInit {
  private passwordService = inject(PasswordService)
  private route = inject(ActivatedRoute)
  private elementRef = inject(ElementRef)
  private renderer = inject(Renderer2)
  private fb = inject(FormBuilder)
  private router = inject(Router)

  protected doNotMatch = signal<string | undefined>(undefined)
  protected error = signal<string | undefined>(undefined)
  protected success = signal<string | undefined>(undefined)
  protected key = signal<string | undefined>(undefined)
  protected invalidKey = signal(false)
  protected keyMissing = signal(false)
  protected expiredKey = signal(false)
  protected activationEmailResent = signal(false)
  protected showPasswordForm = signal(false)

  passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
  })

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.key.set(params['key'])
    })
    this.keyMissing.set(!this.key())
    this.passwordService.validateKey({ key: this.key() }).subscribe((res) => {
      this.expiredKey.set(res.expiredKey)
      this.invalidKey.set(res.invalidKey)
      this.showPasswordForm.set(!this.invalidKey() && !this.expiredKey())
      if (this.expiredKey()) {
        this.passwordService.resendActivationEmail({ key: this.key() }).subscribe((result) => {
          this.activationEmailResent.set(result.resent)
        })
      }
    })
  }

  ngAfterViewInit() {
    if (this.elementRef.nativeElement.querySelector('#password') != null) {
      this.renderer.selectRootElement('#password').scrollIntoView()
    }
  }

  finishReset() {
    this.doNotMatch.set(undefined)
    this.error.set(undefined)
    const password = this.passwordForm.get(['newPassword'])?.value
    const confirmPassword = this.passwordForm.get(['confirmPassword'])?.value
    if (password !== confirmPassword) {
      this.doNotMatch.set('ERROR')
    } else {
      this.passwordService.savePassword(this.key()!, password).subscribe({
        next: () => {
          this.success.set('OK')
        },
        error: () => {
          this.success.set(undefined)
          this.error.set('ERROR')
        },
      })
    }
  }

  navigateToLoginPage() {
    this.router.navigate(['/login'])
  }
}
