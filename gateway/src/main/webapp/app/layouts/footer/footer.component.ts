import { Component } from '@angular/core';
import { AccountService } from 'app/core';

@Component({
  selector: 'jhi-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['footer.scss']
})
export class FooterComponent {
  constructor(private accountService: AccountService) {}

  isAuthenticated() {
    return this.accountService.isAuthenticated();
  }
}
