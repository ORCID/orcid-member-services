import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core'
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms'
import { AccountService } from '../service/account.service'
import { DomSanitizer } from '@angular/platform-browser'
import { LanguageService } from 'src/app/shared/service/language.service'
import { IAccount } from '../model/account.model'
import { ErrorAlertComponent } from '../../error/error-alert.component'
import { FindLanguageFromKeyPipe } from '../../shared/pipe/find-language-from-key'

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
  imports: [ErrorAlertComponent, ReactiveFormsModule, FindLanguageFromKeyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsComponent implements OnInit {
  private accountService = inject(AccountService)
  private fb = inject(FormBuilder)
  private languageService = inject(LanguageService)
  private sanitizer = inject(DomSanitizer)

  protected account = signal<IAccount | undefined>(undefined)
  protected error = signal<string | undefined>(undefined)
  protected success = signal<string | undefined>(undefined)
  protected languages = signal<any[] | undefined>(undefined)
  private username = signal<string | null>(null)
  protected mfaSetup = signal<any>(undefined)
  protected showMfaSetup = signal<boolean | undefined>(undefined)
  protected showMfaTextCode = signal<boolean | undefined>(undefined)
  protected mfaSetupFailure = signal<boolean | undefined>(undefined)
  protected mfaBackupCodes = signal<string[] | undefined>(undefined)
  protected showMfaBackupCodes = signal<boolean | undefined>(undefined)
  protected showMfaUpdated = signal<boolean | undefined>(undefined)
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

  ngOnInit() {
    this.showMfaSetup.set(false)
    this.showMfaTextCode.set(false)
    this.showMfaBackupCodes.set(false)
    this.accountService.getAccountData(true).subscribe((account) => {
      if (account) {
        this.account.set(account)
        this.updateForm(account)
        this.updateMfaForm(account)
        this.username.set(this.accountService.getUsername())
        if (account && !account.mfaEnabled) {
          this.accountService.getMfaSetup().subscribe((res) => {
            this.mfaSetup.set(res)
          })
        }
      }
    })
    this.languages.set(Object.keys(this.languageService.getAllLanguages()))
  }

  mfaEnabledStateChange(): void {
    this.showMfaUpdated.set(false)
    const mfaEnabled = this.mfaForm.get('mfaEnabled')!.value
    if (mfaEnabled && this.mfaSetup()) {
      this.showMfaSetup.set(true)
      this.showMfaBackupCodes.set(false)
    } else {
      this.showMfaSetup.set(false)
      this.showMfaBackupCodes.set(false)
    }
  }

  toggleMfaTextCode(): void {
    this.showMfaTextCode.set(true)
  }

  save() {
    const settingsAccount = this.accountFromForm()
    this.accountService.save(settingsAccount).subscribe((success: boolean) => {
      if (success) {
        this.error.set(undefined)
        this.success.set('OK')
        this.accountService.getAccountData(true).subscribe((account) => {
          if (account) {
            if (settingsAccount.langKey !== account.langKey) {
              location.reload()
            }
            this.updateForm(account)
            this.updateMfaForm(account)
          }
        })
      } else {
        this.success.set(undefined)
        this.error.set('ERROR')
      }
    })
  }

  saveMfa() {
    const enabled = this.mfaForm.get('mfaEnabled')!.value
    if (enabled) {
      const otp = this.mfaForm.get('verificationCode')!.value
      const setup = { ...this.mfaSetup(), otp }
      this.accountService.enableMfa(setup).subscribe((codes: string[] | null) => {
        if (codes) {
          this.mfaBackupCodes.set(codes)
          this.showMfaBackupCodes.set(true)
          this.showMfaUpdated.set(true)
        } else {
          this.mfaSetupFailure.set(true)
        }
      })
    } else {
      const account = this.account()
      if (account && account.id) {
        this.accountService.disableMfa(account.id).subscribe({
          next: () => {
            this.showMfaUpdated.set(true)
            this.accountService.getMfaSetup().subscribe((res) => {
              this.mfaSetup.set(res)
            })
          },
          error: (err) => console.error('error disabling mfa', err),
        })
      }
    }
  }

  safeQrCode() {
    return this.sanitizer.bypassSecurityTrustResourceUrl('data:image/png;base64, ' + this.mfaSetup().qrCode)
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
