import { Component, OnInit } from '@angular/core'
import { FormBuilder, Validators } from '@angular/forms'
import { AccountService } from '../service/account.service'
import { DomSanitizer } from '@angular/platform-browser'
import { LanguageService } from 'src/app/shared/service/language.service'
import { IAccount } from '../model/account.model'

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
})
export class SettingsComponent implements OnInit {
  error: string | undefined
  success: string | undefined
  languages: any[] | undefined
  userName: string | null = null
  mfaSetup: any
  showMfaSetup: boolean | undefined
  showMfaTextCode: boolean | undefined
  mfaSetupFailure: boolean | undefined
  mfaBackupCodes: string[] | undefined
  showMfaBackupCodes: boolean | undefined
  showMfaUpdated: boolean | undefined
  settingsForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email]],
    activated: [false],
    authorities: [['']],
    langKey: ['en'],
    imageUrl: [''],
  })
  mfaForm = this.fb.group({
    mfaEnabled: false,
    verificationCode: [''],
    securitySave: [],
  })

  constructor(
    private accountService: AccountService,
    private fb: FormBuilder,
    private languageService: LanguageService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.showMfaSetup = false
    this.showMfaTextCode = false
    this.showMfaBackupCodes = false
    console.log('calling get account data')
    this.accountService.getAccountData().subscribe((account) => {
      console.log('got account data', account)
      if (account) {
        this.updateForm(account)
        this.updateMfaForm(account)
        this.userName = this.accountService.getUserName()
        console.log('acocunt and account mfa enabled are ', account, ' and ', account.mfaEnabled)
        if (account && !account.mfaEnabled) {
          this.accountService.getMfaSetup().subscribe((res) => {
            console.log('setting mfa setup to ' + res)
            this.mfaSetup = res
          })
        }
      }
    })
    this.languages = this.languageService.getAllLanguages()
  }

  mfaEnabledStateChange(): void {
    console.log('mfa state change called')
    this.showMfaUpdated = false
    const mfaEnabled = this.mfaForm.get('mfaEnabled')!.value
    console.log('setup is ' + this.mfaSetup)
    if (mfaEnabled && this.mfaSetup) {
      this.showMfaSetup = true
      this.showMfaBackupCodes = false
    } else {
      this.showMfaSetup = false
      this.showMfaBackupCodes = false
    }
  }

  toggleMfaTextCode(): void {
    this.showMfaTextCode = true
  }

  save() {
    const settingsAccount = this.accountFromForm()
    this.accountService.save(settingsAccount).subscribe({
      next: () => {
        this.error = undefined
        this.success = 'OK'
        this.accountService.getAccountData().subscribe((account) => {
          if (account) {
            this.updateForm(account)
            this.updateMfaForm(account)
          }
        })
        this.languageService.getCurrentLanguage().subscribe((current) => {
          if (settingsAccount.langKey !== current) {
            this.languageService.changeLanguage(settingsAccount.langKey)
          }
        })
      },
      error: () => {
        this.success = undefined
        this.error = 'ERROR'
      },
    })
  }

  saveMfa() {
    const enabled = this.mfaForm.get('mfaEnabled')!.value
    if (enabled) {
      const otp = this.mfaForm.get('verificationCode')!.value
      console.log('about to set otp on ' + this.mfaSetup)
      this.mfaSetup.otp = otp
      this.accountService.enableMfa(this.mfaSetup).subscribe({
        next: (res) => {
          this.mfaBackupCodes = res.body
          this.showMfaBackupCodes = true
          this.showMfaUpdated = true
        },
        error: (err) => {
          this.mfaSetupFailure = true
        },
      })
    } else {
      this.accountService.disableMfa().subscribe({
        next: () => {
          this.showMfaUpdated = true
          this.accountService.getMfaSetup().subscribe((res) => {
            this.mfaSetup = res
          })
        },
        error: (err) => console.log('error disabling mfa'),
      })
    }
  }

  safeQrCode() {
    return this.sanitizer.bypassSecurityTrustResourceUrl('data:image/png;base64, ' + this.mfaSetup.qrCode)
  }

  private accountFromForm(): any {
    const account = {}
    return {
      ...account,
      firstName: this.settingsForm.get('firstName')!.value,
      lastName: this.settingsForm.get('lastName')!.value,
      email: this.settingsForm.get('email')!.value,
      activated: this.settingsForm.get('activated')!.value,
      authorities: this.settingsForm.get('authorities')!.value,
      langKey: this.settingsForm.get('langKey')!.value,
      imageUrl: this.settingsForm.get('imageUrl')!.value,
    }
  }

  updateForm(account: IAccount): void {
    this.settingsForm.patchValue({
      firstName: account.firstName,
      lastName: account.lastName,
      email: account.email,
      activated: account.activated,
      authorities: account.authorities,
      langKey: account.langKey,
      imageUrl: account.imageUrl,
    })
  }

  updateMfaForm(account: IAccount): void {
    this.mfaForm.patchValue({
      mfaEnabled: account.mfaEnabled,
    })
  }
}
