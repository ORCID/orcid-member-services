import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core'
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms'
import { PasswordService } from '../service/password.service'
import { AccountService } from '../service/account.service'
import { PasswordStrengthComponent } from './password-strength.component'

@Component({
  selector: 'app-password',
  templateUrl: './password.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, PasswordStrengthComponent],
})
export class PasswordComponent implements OnInit {
  private passwordService = inject(PasswordService)
  private accountService = inject(AccountService)
  private fb = inject(FormBuilder)

  protected doNotMatch = signal<string | undefined>(undefined)
  protected error = signal<string | undefined>(undefined)
  protected success = signal<string | undefined>(undefined)
  protected username = signal<string | undefined | null>(null)
  protected passwordForUsernameString = signal<string | undefined | null>(null)
  protected account = signal<any>(null)
  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
  })

  ngOnInit() {
    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        this.account.set(account)
        this.username.set(this.accountService.getUsername())
        this.passwordForUsernameString.set($localize`:@@password.title.string:Password for ${this.username()} (You)`)
      }
    })
  }

  changePassword() {
    const newPassword = this.passwordForm.get(['newPassword'])?.value
    if (newPassword !== this.passwordForm.get(['confirmPassword'])?.value) {
      this.error.set(undefined)
      this.success.set(undefined)
      this.doNotMatch.set('ERROR')
    } else {
      this.doNotMatch.set(undefined)
      this.passwordService.updatePassword(newPassword, this.passwordForm.get(['currentPassword'])?.value).subscribe({
        next: () => {
          this.error.set(undefined)
          this.success.set('OK')
        },
        error: () => {
          this.success.set(undefined)
          this.error.set('ERROR')
        },
      })
    }
  }
}
