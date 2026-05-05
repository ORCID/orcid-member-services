import { Component, OnInit, inject } from '@angular/core'
import { FormBuilder, FormGroup, Validators } from '@angular/forms'
import { PasswordService } from '../service/password.service'
import { AccountService } from '../service/account.service'

@Component({
  selector: 'app-password',
  templateUrl: './password.component.html',
  standalone: false,
})
export class PasswordComponent implements OnInit {
  private passwordService = inject(PasswordService)
  private accountService = inject(AccountService)
  private fb = inject(FormBuilder)

  doNotMatch: string | undefined
  error: string | undefined
  success: string | undefined
  username: string | undefined | null = null
  passwordForUsernameString: string | undefined | null = null
  account: any
  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
  })

  ngOnInit() {
    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        this.account = account
        this.username = this.accountService.getUsername()
        this.passwordForUsernameString = $localize`:@@password.title.string:Password for ${this.username} (You)`
      }
    })
  }

  changePassword() {
    const newPassword = this.passwordForm.get(['newPassword'])?.value
    if (newPassword !== this.passwordForm.get(['confirmPassword'])?.value) {
      this.error = undefined
      this.success = undefined
      this.doNotMatch = 'ERROR'
    } else {
      this.doNotMatch = undefined
      this.passwordService.updatePassword(newPassword, this.passwordForm.get(['currentPassword'])?.value).subscribe({
        next: () => {
          this.error = undefined
          this.success = 'OK'
        },
        error: () => {
          this.success = undefined
          this.error = 'ERROR'
        },
      })
    }
  }
}
