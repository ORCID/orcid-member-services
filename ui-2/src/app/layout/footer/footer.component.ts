import { Component, inject } from '@angular/core'
import { AccountService } from 'src/app/account/service/account.service'

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
  standalone: false,
})
export class FooterComponent {
  private accountService = inject(AccountService)

  isAuthenticated() {
    return this.accountService.isAuthenticated()
  }

  get releaseVersion(): string | null {
    return this.accountService.getReleaseVersion()
  }
}
