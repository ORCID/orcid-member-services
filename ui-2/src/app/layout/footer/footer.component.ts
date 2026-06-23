import { Component, ChangeDetectionStrategy, inject } from '@angular/core'
import { AccountService } from 'src/app/account/service/account.service'

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  private accountService = inject(AccountService)
  protected readonly currentYear = new Date().getFullYear()

  isAuthenticated() {
    return this.accountService.isAuthenticated()
  }

  get releaseVersion(): string | null {
    return this.accountService.getReleaseVersion()
  }
}
