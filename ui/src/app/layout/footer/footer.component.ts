import { Component } from '@angular/core'
import { AccountService } from 'src/app/account/service/account.service'

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
})
export class FooterComponent {
  constructor(private accountService: AccountService) {}

  isAuthenticated() {
    return this.accountService.isAuthenticated()
  }
}
