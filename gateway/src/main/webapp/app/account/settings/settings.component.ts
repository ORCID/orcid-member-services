import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { JhiLanguageService } from 'ng-jhipster';
import { AccountService, JhiLanguageHelper } from 'app/core';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'jhi-settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {
  error: string;
  success: string;
  languages: any[];
  userName: string;
  mfaSetup: any;
  showMfaSetup: boolean;
  showMfaTextCode: boolean;
  mfaSetupFailure: boolean;
  mfaBackupCodes: string[];
  showMfaBackupCodes: boolean;
  showMfaUpdated: boolean;
  settingsForm = this.fb.group({
    firstName: [undefined, [Validators.required, Validators.minLength(1), Validators.maxLength(50)]],
    lastName: [undefined, [Validators.required, Validators.minLength(1), Validators.maxLength(50)]],
    email: [undefined, [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email]],
    activated: [false],
    authorities: [[]],
    langKey: ['en'],
    imageUrl: []
  });
  mfaForm = this.fb.group({
    mfaEnabled: [[]],
    verificationCode: [],
    securitySave: []
  });

  constructor(
    private accountService: AccountService,
    private fb: FormBuilder,
    private languageService: JhiLanguageService,
    private languageHelper: JhiLanguageHelper,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.showMfaSetup = false;
    this.showMfaTextCode = false;
    this.showMfaBackupCodes = false;
    this.accountService.identity(true).then(account => {
      this.updateForm(account);
      this.updateMfaForm(account);
      this.userName = this.accountService.getUserName();
      if (!account.mfaEnabled) {
        this.accountService.getMfaSetup().subscribe(res => {
          this.mfaSetup = res.body;
        });
      }
    });
    this.languageHelper.getAll().then(languages => {
      this.languages = languages;
    });
  }

  mfaEnabledStateChange(): void {
    this.showMfaUpdated = false;
    const mfaEnabled = this.mfaForm.get('mfaEnabled').value;
    console.log('setup is ' + this.mfaSetup);
    if (mfaEnabled && this.mfaSetup) {
      this.showMfaSetup = true;
      this.showMfaBackupCodes = false;
    } else {
      this.showMfaSetup = false;
      this.showMfaBackupCodes = false;
    }
  }

  toggleMfaTextCode(): void {
    this.showMfaTextCode = true;
  }

  save() {
    const settingsAccount = this.accountFromForm();
    this.accountService.save(settingsAccount).subscribe(
      () => {
        this.error = null;
        this.success = 'OK';
        this.accountService.identity(true).then(account => {
          this.updateForm(account);
          this.updateMfaForm(account);
        });
        this.languageService.getCurrent().then(current => {
          if (settingsAccount.langKey !== current) {
            this.languageService.changeLanguage(settingsAccount.langKey);
          }
        });
      },
      () => {
        this.success = null;
        this.error = 'ERROR';
      }
    );
  }

  saveMfa() {
    const enabled = this.mfaForm.get('mfaEnabled').value;
    if (enabled) {
      const otp = this.mfaForm.get('verificationCode').value;
      this.mfaSetup.otp = otp;
      this.accountService.enableMfa(this.mfaSetup).subscribe(
        res => {
          this.mfaBackupCodes = res.body;
          this.showMfaBackupCodes = true;
          this.showMfaUpdated = true;
        },
        err => {
          this.mfaSetupFailure = true;
        }
      );
    } else {
      this.accountService.disableMfa().subscribe(
        () => {
          this.showMfaUpdated = true;
          this.accountService.getMfaSetup().subscribe(res => {
            this.mfaSetup = res.body;
          });
        },
        err => console.log('error disabling mfa')
      );
    }
  }

  safeQrCode() {
    return this.sanitizer.bypassSecurityTrustResourceUrl('data:image/png;base64, ' + this.mfaSetup.qrCode);
  }

  private accountFromForm(): any {
    const account = {};
    return {
      ...account,
      firstName: this.settingsForm.get('firstName').value,
      lastName: this.settingsForm.get('lastName').value,
      email: this.settingsForm.get('email').value,
      activated: this.settingsForm.get('activated').value,
      authorities: this.settingsForm.get('authorities').value,
      langKey: this.settingsForm.get('langKey').value,
      imageUrl: this.settingsForm.get('imageUrl').value
    };
  }

  updateForm(account: any): void {
    this.settingsForm.patchValue({
      firstName: account.firstName,
      lastName: account.lastName,
      email: account.email,
      activated: account.activated,
      authorities: account.authorities,
      langKey: account.langKey,
      imageUrl: account.imageUrl
    });
  }

  updateMfaForm(account: any): void {
    this.mfaForm.patchValue({
      mfaEnabled: account.mfaEnabled
    });
  }
}
