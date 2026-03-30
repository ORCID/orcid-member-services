import { AfterViewInit, Component, NgZone, OnDestroy, Renderer2 } from '@angular/core'
import { FormBuilder } from '@angular/forms'
import { Router } from '@angular/router'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { Subscription } from 'rxjs'
import { EventType } from 'src/app/app.constants'
import { Event } from 'src/app/shared/model/event.model'
import { EventService } from 'src/app/shared/service/event.service'
import { StateStorageService } from '../service/state-storage.service'
import { LoginService } from '../service/login.service'
import { ILoginCredentials } from '../model/login.model'

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements AfterViewInit, OnDestroy {
  authenticationError = false
  showMfa = false
  mfaError = false
  sub: Subscription | undefined

  loginForm = this.fb.group({
    username: [''],
    password: [''],
    mfaCode: [''],
  })

  get username() {
    return this.loginForm.get('username')
  }

  get password() {
    return this.loginForm.get('password')
  }

  get mfaCode() {
    return this.loginForm.get('mfaCode')
  }

  constructor(
    private loginService: LoginService,
    private stateStorageService: StateStorageService,
    private renderer: Renderer2,
    private router: Router,
    private oidcSecurityService: OidcSecurityService,
    private fb: FormBuilder,
    private eventService: EventService,
    private ngZone: NgZone
  ) {
    this.sub = this.eventService.on(EventType.LOG_IN_SUCCESS).subscribe()
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
      username: this.username?.value || '',
      password: this.password?.value || '',
      mfaCode: this.mfaCode?.value
    }

    if (!credentials.username && !credentials.password) {
      this.authenticationError = true
      return
    }

    // If we are showing MFA but no code is entered, stop here
    if (this.showMfa && !credentials.mfaCode) {
      this.mfaError = true
      return
    }

    this.loginService.login(credentials).subscribe({
      next: (res) => {
        if (res.status === 'success') {
          this.showMfa = false
          this.oidcSecurityService.authorize()
        }
      },
      error: (err) => {
        if (err.status === 401) {
          this.authenticationError = true
          console.log('MFA is required, showing MFA input', err.error)
          if (err.error?.error === 'mfa_required') {
            this.showMfa = true
          }
          if (err.error?.error === 'mfa_invalid') {
            this.showMfa = true
            this.mfaError  = true
          }
        } else {
            this.authenticationError = true
            this.loginService.logout()
          }
      },
    })
  }

  loginSuccess(): void {
    this.ngZone.run(() => {
      this.router.navigate([''])
    })

    this.eventService.broadcast(new Event(EventType.LOG_IN_SUCCESS))

    const redirect = this.stateStorageService.getUrl()
    if (redirect) {
      console.log('Redirecting to stored url after login:', redirect)
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
