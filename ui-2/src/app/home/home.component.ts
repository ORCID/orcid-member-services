import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { AccountService } from '../account'
import { ISFMemberData } from '../member/model/salesforce-member-data.model'
import { IAccount } from '../account/model/account.model'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { RouterOutlet } from '@angular/router'
import { ContactUpdateAlertComponent } from '../shared/alert/contact-update/contact-update-alert.component'
import { AddConsortiumMemberAlertComponent } from '../shared/alert/consortium-member/add-consortium-member-alert.component'
import { RemoveConsortiumMemberAlertComponent } from '../shared/alert/consortium-member/remove-consortium-member-alert.component'

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [
    RouterOutlet,
    ContactUpdateAlertComponent,
    AddConsortiumMemberAlertComponent,
    RemoveConsortiumMemberAlertComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit {
  private readonly accountService = inject(AccountService)
  private readonly oidcSecurityService = inject(OidcSecurityService)
  private readonly destroyRef = inject(DestroyRef)

  private readonly accountState = signal<IAccount | undefined | null>(null)
  private readonly memberDataState = signal<ISFMemberData | undefined | null>(null)
  private readonly salesforceIdState = signal<string | undefined>(undefined)
  protected readonly loggedInMessageState = signal<string | undefined>(undefined)

  protected get loggedInMessage(): string | undefined {
    return this.loggedInMessageState()
  }

  ngOnInit() {
    this.oidcSecurityService.checkAuth().subscribe(({ isAuthenticated }) => {
      if (isAuthenticated) {
        this.accountService.getAccountData()
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((account) => {
            this.accountState.set(account)
            if (account) {
              this.loggedInMessageState.set(
                $localize`:@@home.loggedIn.message.string:You are logged in as user ${account.email}`
              )
            }
          })
      }
    })
  }
}
