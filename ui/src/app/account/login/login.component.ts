import { AfterViewInit, Component, NgZone, OnDestroy, Renderer2 } from '@angular/core'
import { FormBuilder } from '@angular/forms'
import { Router } from '@angular/router'
import { AccountService } from '../service/account.service'
import { LoginService } from '../service/login.service'
import { StateStorageService } from '../service/state-storage.service'
import { Subscription, filter, take } from 'rxjs'
import { EventService } from 'src/app/shared/service/event.service'
import { EventType } from 'src/app/app.constants'
import { Event } from 'src/app/shared/model/event.model'
import { ILoginCredentials } from '../model/login.model'
import { OidcSecurityService } from 'angular-auth-oidc-client'

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements AfterViewInit, OnDestroy {
  authenticationError = false
  showMfa = false
  mfaSent = false
  mfaError = false
  sub: Subscription | undefined

  loginForm = this.fb.group({
    username: [''],
    password: [''],
    mfaCode: [''],
  })

  constructor(
    private loginService: LoginService,
    private stateStorageService: StateStorageService,
    private renderer: Renderer2,
    private router: Router,
    private accountService: AccountService,
    private oidcSecurityService: OidcSecurityService,
    private fb: FormBuilder,
    private eventService: EventService,
    private ngZone: NgZone
  ) {
    this.sub = this.eventService.on(EventType.LOG_IN_SUCCESS).subscribe((e) => {
      console.log('login success')
    })
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe()
  }

  ngAfterViewInit() {
    setTimeout(() => this.renderer.selectRootElement('#username').scrollIntoView())
  }

  cancel() {
    this.authenticationError = false
    this.loginForm.patchValue({
      username: '',
      password: '',
      mfaCode: '',
    })
  }

  login() {
    this.authenticationError = false
    this.mfaError = false

    const credentials: ILoginCredentials = {
      username: this.loginForm.get('username')!.value!,
      password: this.loginForm.get('password')!.value!,
      mfaCode: this.loginForm.get('mfaCode')?.value,
    }

    // If we are showing MFA but no code is entered, stop here
    if (this.showMfa && !credentials.mfaCode) {
      this.mfaError = true
      return
    }

    this.loginService.login(credentials).subscribe({
      next: (res) => {
        // res comes from our MyCustomSuccessHandler: {"status": "success", "redirectUrl": "..."}
        if (res.status === 'success') {
          this.showMfa = false
          // STEP 2: Trigger the OIDC PKCE Handshake
          // The library will handle the redirect to localhost:9000/oauth2/authorize
          this.oidcSecurityService.authorize()
        }
      },
      error: (err) => {
        // Catch our custom 401 MfaRequiredException
        if (err.status === 401 && err.error?.error === 'mfa_required') {
          this.showMfa = true
          this.mfaError = true
        } else {
          this.authenticationError = true
          this.loginService.logout()
        }
        this.mfaSent = false
      },
    })
  }

  loginSuccess(): void {
    this.ngZone.run(() => {
      this.router.navigate([''])
    })

    this.eventService.broadcast(new Event(EventType.LOG_IN_SUCCESS))

    // previousState was set in the authExpiredInterceptor before being redirected to login modal.
    // since login is successful, go to stored previousState and clear previousState
    const redirect = this.stateStorageService.getUrl()
    if (redirect) {
      this.stateStorageService.storeUrl(null)
      this.router.navigateByUrl(redirect)
    }
  }

  register() {
    this.router.navigate(['/register'])
  }

  requestResetPassword() {
    this.router.navigate(['/reset', 'request'])
  }
}
