import { Component, OnInit } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { Router } from '@angular/router'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { AccountService, StateStorageService } from './account'
import { EventService } from './shared/service/event.service'
import { EventType } from './app.constants'
import { Event } from './shared/model/event.model'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  constructor(
    private oidcSecurityService: OidcSecurityService,
    private accountService: AccountService,
    private eventService: EventService,
    private stateStorageService: StateStorageService,
    private router: Router
  ) {}

  ngOnInit() {
    this.oidcSecurityService.checkAuth().subscribe(({ isAuthenticated, accessToken, userData }) => {
      if (isAuthenticated) {
        console.log('OIDC Authentication Successful')

        this.accountService.getAccountData(true).subscribe(() => {
          this.eventService.broadcast(new Event(EventType.LOG_IN_SUCCESS))

          const redirect = this.stateStorageService.getUrl()
          if (redirect) {
            this.stateStorageService.storeUrl(null)
            this.router.navigateByUrl(redirect)
          } else if (this.router.url.includes('login/callback')) {
            this.router.navigate(['/'])
          }
        })
      }
    })
  }
}
