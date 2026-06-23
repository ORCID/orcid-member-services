import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core'
import { RouterOutlet } from '@angular/router'
import { Router } from '@angular/router'
import { OidcSecurityService } from 'angular-auth-oidc-client'
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
  private readonly authCheckStarted = signal(false)

  ngOnInit() {
    if (!window.location.pathname.includes('/landing-page')) {
      this.authCheckStarted.set(true)
      this.oidcSecurityService.checkAuth().subscribe(({ isAuthenticated, accessToken, errorMessage }) => {
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
        } else {
          console.error('OIDC Authentication FAILED or NOT LOGGED IN')
          console.error('Error Message:', errorMessage)
        }
      })
    }
  }
}
