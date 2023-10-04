import { AfterViewInit, Component, ElementRef, Renderer2 } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { AccountService } from '../service/account.service';
import { LoginService } from '../service/login.service';
import { StateStorageService } from '../service/state-storage.service';
import { ILoginResult } from '../model/login.model';
import { IAccount } from '../model/account.model';
import { filter, take } from 'rxjs';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements AfterViewInit {
  authenticationError = false;
  showMfa = false;
  mfaSent = false;
  mfaError = false;

  loginForm = this.fb.group({
    username: [''],
    password: [''],
    rememberMe: [true],
    mfaCode: ['']
  });

  constructor(
    private loginService: LoginService,
    private stateStorageService: StateStorageService,
    private elementRef: ElementRef,
    private renderer: Renderer2,
    private router: Router,
    private accountService: AccountService,
    private fb: FormBuilder
  ) { }

  ngAfterViewInit() {
    setTimeout(() => this.renderer.selectRootElement('#username').scrollIntoView());
  }

  cancel() {
    this.authenticationError = false;
    this.loginForm.patchValue({
      username: '',
      password: '',
      mfaCode: ''
    });
  }

  login() {
    this.mfaError = false;
    const mfaCode = this.loginForm.get('mfaCode')?.value;

    if (this.showMfa && !mfaCode) {
      this.mfaError = true;
    } else {
      if (mfaCode) {
        this.mfaSent = true;
      }

      this.loginService
        .login({
          username: this.loginForm.get('username')?.value!,
          password: this.loginForm.get('password')?.value!,
          mfaCode: this.loginForm.get('mfaCode')?.value
        })
        .subscribe(
          {
            next: (data: ILoginResult) => {
              if (!data.mfaRequired) {
                this.showMfa = false;
                this.accountService.getAccountData().pipe(filter((account: IAccount | undefined) => !!account), take(1)).subscribe((account: IAccount | undefined) => {
                  console.log("Login successful, account data:", account);
                  this.loginSuccess();
                });
              } else {
                this.showMfa = true;
                this.mfaError = this.mfaSent;
              }
              this.mfaSent = false;
            },
            // TODO: review any type
            error: err => {
              this.loginService.logout();
              this.authenticationError = true;
            }
          }
        );
    }
  }

  loginSuccess(): void {
    if (this.router.url === '/register' || /^\/activate\//.test(this.router.url) || /^\/reset\//.test(this.router.url)) {
      this.router.navigate(['']);
    }

    // TODO: Event manager
    /*  this.eventManager.broadcast({
       name: 'authenticationSuccess',
       content: 'Sending Authentication Success'
     }); */

    // previousState was set in the authExpiredInterceptor before being redirected to login modal.
    // since login is successful, go to stored previousState and clear previousState
    const redirect = this.stateStorageService.getUrl();
    if (redirect) {
      this.stateStorageService.storeUrl(null);
      this.router.navigateByUrl(redirect);
    }
  }

  register() {
    this.router.navigate(['/register']);
  }

  requestResetPassword() {
    this.router.navigate(['/reset', 'request']);
  }

}

