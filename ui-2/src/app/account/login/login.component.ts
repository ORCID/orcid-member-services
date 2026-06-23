import { AfterViewInit, ChangeDetectionStrategy, Component, DestroyRef, ElementRef, NgZone, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormBuilder, ReactiveFormsModule } from '@angular/forms'
import { Router } from '@angular/router'
import { OidcSecurityService } from 'angular-auth-oidc-client'
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
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent implements AfterViewInit {
  private readonly loginService = inject(LoginService)
  private readonly stateStorageService = inject(StateStorageService)
  private readonly elementRef = inject(ElementRef<HTMLElement>)
  private readonly router = inject(Router)
  private readonly oidcSecurityService = inject(OidcSecurityService)
  private readonly fb = inject(FormBuilder)
  private readonly eventService = inject(EventService)
  private readonly ngZone = inject(NgZone)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly authenticationErrorState = signal(false)
  protected readonly showMfaState = signal(false)
  protected readonly mfaErrorState = signal(false)

  protected readonly loginForm = this.fb.group({
    username: [''],
    password: [''],
    mfaCode: [''],
  })

  protected get authenticationError(): boolean {
    return this.authenticationErrorState()
  }

  protected get showMfa(): boolean {
    return this.showMfaState()
  }

  protected get mfaError(): boolean {
    return this.mfaErrorState()
  }

  protected get username() {
    return this.loginForm.get('username')
  }

  protected get password() {
    return this.loginForm.get('password')
  }

  protected get mfaCode() {
    return this.loginForm.get('mfaCode')
  }

  constructor() {
    this.eventService.on(EventType.LOG_IN_SUCCESS).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()
  }

  ngAfterViewInit() {
    setTimeout(() => {
      const usernameField = (this.elementRef.nativeElement as HTMLElement).querySelector(
        '#username'
      ) as HTMLElement | null
      usernameField?.scrollIntoView()
    })
  }

  protected cancel() {
    this.authenticationErrorState.set(false)
    this.loginForm.patchValue({
      username: '',
      password: '',
      mfaCode: '',
    })
  }

  protected login() {
    this.authenticationErrorState.set(false)
    this.mfaErrorState.set(false)

    const credentials: ILoginCredentials = {
      username: this.username?.value || '',
      password: this.password?.value || '',
      mfaCode: this.mfaCode?.value,
    }

    if (!credentials.username && !credentials.password) {
      this.authenticationErrorState.set(true)
      return
    }

    // If we are showing MFA but no code is entered, stop here
    if (this.showMfaState() && !credentials.mfaCode) {
      this.mfaErrorState.set(true)
      return
    }

    this.loginService.login(credentials).subscribe({
      next: (res) => {
        if (res.status === 'success') {
          this.showMfaState.set(false)
          this.oidcSecurityService.authorize()
        }
      },
      error: (err) => {
        if (err.status === 401) {
          if (err.error?.error === 'mfa_required' || err.error?.error === 'mfa_invalid') {
            if (err.error?.error === 'mfa_required') {
              this.showMfaState.set(true)
            }
            if (err.error?.error === 'mfa_invalid') {
              this.showMfaState.set(true)
              this.mfaErrorState.set(true)
            }
          } else {
            this.authenticationErrorState.set(true)
          }
        } else {
          this.authenticationErrorState.set(true)
          this.loginService.logout()
        }
      },
    })
  }

  protected loginSuccess(): void {
    this.ngZone.run(() => {
      this.router.navigate([''])
    })

    this.eventService.broadcast(new Event(EventType.LOG_IN_SUCCESS))

    const redirect = this.stateStorageService.getUrl()
    if (redirect) {
      this.stateStorageService.storeUrl(null)
      this.router.navigateByUrl(redirect)
    }
  }

  protected register() {
    this.router.navigate(['/register'])
  }

  protected requestResetPassword() {
    this.router.navigate(['/reset', 'request'])
  }
}
