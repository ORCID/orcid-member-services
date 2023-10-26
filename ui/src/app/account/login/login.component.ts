import { AfterViewInit, Component, OnDestroy, Renderer2 } from '@angular/core'
import { FormBuilder } from '@angular/forms'
import { Router } from '@angular/router'
import { AccountService } from '../service/account.service'
import { LoginService } from '../service/login.service'
import { StateStorageService } from '../service/state-storage.service'
import { Subscription, filter, take } from 'rxjs'
import { EventService } from 'src/app/shared/service/event.service'
import { EventType } from 'src/app/app.constants'
import { Event } from 'src/app/shared/model/event.model'

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
    private fb: FormBuilder,
    private eventService: EventService
  ) {
    this.sub = this.eventService.on(EventType.LOG_IN_SUCCESS).subscribe((e) => {
      console.log(e.payload)
    })
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe()
    console.log('test')
  }

  ngAfterViewInit() {
    setTimeout(() =>
      this.renderer.selectRootElement('#username').scrollIntoView()
    )
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
    this.mfaError = false
    const mfaCode = this.loginForm.get('mfaCode')?.value

    if (this.showMfa && !mfaCode) {
      this.mfaError = true
    } else {
      if (mfaCode) {
        this.mfaSent = true
      }

      this.loginService
        .login({
          username: this.loginForm.get('username')!.value!,
          password: this.loginForm.get('password')!.value!,
          mfaCode: this.loginForm.get('mfaCode')?.value,
        })
        .subscribe({
          next: (data) => {
            if (!data.mfaRequired) {
              this.showMfa = false
              this.accountService
                .getAccountData()
                .pipe(
                  filter((account) => !!account),
                  take(1)
                )
                .subscribe((account) => {
                  // TODO: remove after sprint review
                  console.log('Login successful, account data:', account)
                  this.loginSuccess()
                })
            } else {
              this.showMfa = true
              this.mfaError = this.mfaSent
            }
            this.mfaSent = false
          },
          // TODO: review any type
          error: (err) => {
            this.loginService.logout()
            this.authenticationError = true
          },
        })
    }
  }

  loginSuccess(): void {
    this.router.navigate([''])

    this.eventService.broadcast(
      new Event(EventType.LOG_IN_SUCCESS, 'logged in')
    )

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
