import { Component, ChangeDetectionStrategy, inject } from '@angular/core'
import { Observable } from 'rxjs'
import { AccountService } from 'src/app/account/service/account.service'
import { AsyncPipe } from '@angular/common'

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AsyncPipe],
})
export class FooterComponent {
  private accountService = inject(AccountService)
  protected readonly currentYear = new Date().getFullYear()

  isAuthenticated() {
    return this.accountService.isAuthenticated()
  }

  get releaseVersion(): Observable<string | null> {
    return this.accountService.getReleaseVersion()
  }
}
