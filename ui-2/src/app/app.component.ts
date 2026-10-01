import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { RouterOutlet } from '@angular/router'
import { Router } from '@angular/router'
import { filter } from 'rxjs'
import { EventTypes, OidcSecurityService, PublicEventsService } from 'angular-auth-oidc-client'
import { AccountService, StateStorageService } from './account'
import { EventService } from './shared/service/event.service'
import { EventType } from './app.constants'
import { Event } from './shared/model/event.model'
import { FooterComponent } from './layout/footer/footer.component'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, FooterComponent],
})
export class AppComponent implements OnInit {
  private readonly oidcSecurityService = inject(OidcSecurityService)
  private readonly accountService = inject(AccountService)
  private readonly eventService = inject(EventService)
  private readonly stateStorageService = inject(StateStorageService)
  private readonly router = inject(Router)
  private readonly publicEventsService = inject(PublicEventsService)
  private readonly destroyRef = inject(DestroyRef)
  private readonly authCheckStarted = signal(false)

  ngOnInit() {
    // the auth server issues no refresh tokens to the UI client, so renewal fails once the access token expires
    this.publicEventsService
      .registerForEvents()
      .pipe(
        filter((event) => event.type === EventTypes.SilentRenewFailed),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.handleSessionExpired())

    if (!window.location.pathname.includes('/landing-page')) {
      this.authCheckStarted.set(true)
      this.oidcSecurityService.checkAuth().subscribe(({ isAuthenticated, errorMessage }) => {
        if (isAuthenticated) {
          this.accountService.getAccountData(true).subscribe(() => {
            this.eventService.broadcast(new Event(EventType.LOG_IN_SUCCESS))

            const redirect = this.stateStorageService.getUrl()
            if (redirect) {
              this.stateStorageService.storeUrl(null)
              this.router.navigateByUrl(redirect)
            } else if (this.router.url.includes('auth/callback')) {
              this.router.navigate(['/'])
            }
          })
        } else if (errorMessage) {
          console.error('OIDC Authentication failed:', errorMessage)
        }
      })
    }
  }

  private handleSessionExpired() {
    const currentUrl = this.router.url
    if (currentUrl.startsWith('/login') || currentUrl.startsWith('/landing-page')) {
      return
    }
    this.accountService.clearAccountData()
    this.oidcSecurityService.logoffLocal()
    this.stateStorageService.storeUrl(currentUrl)
    this.router.navigate(['/login'])
  }
}
