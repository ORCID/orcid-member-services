import { AsyncPipe } from '@angular/common'
import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { Observable, of } from 'rxjs'
import { map } from 'rxjs/operators'
import { AccountService } from 'src/app/account/service/account.service'

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AsyncPipe],
})
export class FooterComponent {
  private accountService = inject(AccountService)
  private oidcSecurityService = inject(OidcSecurityService)
  private authState$ = (this.oidcSecurityService as any).isAuthenticated$ ?? of({ isAuthenticated: false })
  protected readonly currentYear = new Date().getFullYear()
  protected readonly isAuthenticated = toSignal(
    this.authState$.pipe(map((authState: any) => !!authState?.isAuthenticated)),
    { initialValue: false }
  )

  get releaseVersion(): Observable<string | null> {
    return this.accountService.getReleaseVersion()
  }
}
