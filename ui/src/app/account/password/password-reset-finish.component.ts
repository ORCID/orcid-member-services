import { PasswordService } from '../service/password.service'
import { Component, OnInit, AfterViewInit, Renderer2, ElementRef } from '@angular/core'
import { FormBuilder, FormGroup, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'


@Component({
  selector: 'app-password-reset-finish',
  templateUrl: './password-reset-finish.component.html',
})
export class PasswordResetFinishComponent implements OnInit, AfterViewInit {
  doNotMatch: string | undefined
  error: string | undefined
  success: string | undefined
  key: string | undefined
  invalidKey = false
  keyMissing = false
  expiredKey = false
  activationEmailResent = false
  showPasswordForm = false

  passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
  })

  constructor(
    private passwordService: PasswordService,
    private route: ActivatedRoute,
    private elementRef: ElementRef,
    private renderer: Renderer2,
    private fb: FormBuilder,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.key = params['key']
    })
    this.keyMissing = !this.key
    this.passwordService.validateKey({ key: this.key }).subscribe((res) => {
      this.expiredKey = res.expiredKey
      this.invalidKey = res.invalidKey
      this.showPasswordForm = !this.invalidKey && !this.expiredKey
      if (this.expiredKey) {
        this.passwordService.resendActivationEmail({ key: this.key }).subscribe((result) => {
          this.activationEmailResent = result.resent
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
    this.doNotMatch = undefined
    this.error = undefined
    const password = this.passwordForm.get(['newPassword'])?.value
    const confirmPassword = this.passwordForm.get(['confirmPassword'])?.value
    if (password !== confirmPassword) {
      this.doNotMatch = 'ERROR'
    } else {
      this.passwordService.savePassword(this.key!, password).subscribe({
        next: () => {
          this.success = 'OK'
        },
        error: () => {
          this.success = undefined
          this.error = 'ERROR'
        }
    })
    }
  }

  navigateToLoginPage() {
    this.router.navigate(['/login'])
  }
}
