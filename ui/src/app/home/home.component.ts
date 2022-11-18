import { AfterViewInit, Component, ElementRef, OnInit, Renderer2 } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Router } from '@angular/router';
import { JhiEventManager } from 'ng-jhipster';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/core/login/login.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';

@Component({
  selector: 'app-home',
  templateUrl: './login.component.html'
})
export class HomeComponent implements AfterViewInit, OnInit {
  authenticationError: boolean;
  isModal = false;
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
    private eventManager: JhiEventManager,
    private loginService: LoginService,
    private stateStorageService: StateStorageService,
    private elementRef: ElementRef,
    private renderer: Renderer2,
    private router: Router,
    private accountService: AccountService,
    public activeModal: NgbActiveModal,
    private fb: FormBuilder
  ) {}

  ngAfterViewInit() {
    setTimeout(() => this.renderer.invokeElementMethod(this.elementRef.nativeElement.querySelector('#username'), 'focus', []), 0);
  }

  cancel() {
    this.authenticationError = false;
    this.loginForm.patchValue({
      username: '',
      password: '',
      mfaCode: ''
    });
    if (!this.isModal) {
      this.activeModal.dismiss('cancel');
    }
  }

  login() {
    this.mfaError = false;
    const mfaCode = this.loginForm.get('mfaCode').value;

    if (this.showMfa && !mfaCode) {
      this.mfaError = true;
    } else {
      if (mfaCode) {
        this.mfaSent = true;
      }

      this.loginService
        .login({
          username: this.loginForm.get('username').value,
          password: this.loginForm.get('password').value,
          rememberMe: this.loginForm.get('rememberMe').value,
          mfaCode: this.loginForm.get('mfaCode').value
        })
        .subscribe(
          data => {
            if (!data.mfaRequired) {
              this.showMfa = false;
              this.accountService.identity(true).then(account => {
                this.loginSuccess();
              });
            } else {
              this.showMfa = true;
              this.mfaError = this.mfaSent;
            }
            this.mfaSent = false;
          },
          err => {
            this.loginService.logout();
            this.authenticationError = true;
          }
        );
    }
  }

  loginSuccess(): void {
    if (!this.isModal) {
      this.activeModal.close('login success');
    }
    if (this.router.url === '/register' || /^\/activate\//.test(this.router.url) || /^\/reset\//.test(this.router.url)) {
      this.router.navigate(['']);
    }

    this.eventManager.broadcast({
      name: 'authenticationSuccess',
      content: 'Sending Authentication Success'
    });

    // previousState was set in the authExpiredInterceptor before being redirected to login modal.
    // since login is successful, go to stored previousState and clear previousState
    const redirect = this.stateStorageService.getUrl();
    if (redirect) {
      this.stateStorageService.storeUrl(null);
      this.router.navigateByUrl(redirect);
    }
  }

  register() {
    if (this.isModal) {
      this.activeModal.dismiss('to state register');
    }
    this.router.navigate(['/register']);
  }

  requestResetPassword() {
    if (this.isModal) {
      this.activeModal.dismiss('to state requestReset');
    }
    this.router.navigate(['/reset', 'request']);
  }

  ngOnInit() {
    // We don't show signin in a modal currently
    // Add logic to change isModal here if needed
  }
}
