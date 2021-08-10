import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IAssertion } from 'app/shared/model/AssertionService/assertion.model';
import { ORCID_BASE_URL } from 'app/app.constants';
import { UserService } from 'app/core';
import { ClipboardModule } from 'ngx-clipboard';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'jhi-assertion-detail',
  templateUrl: './assertion-detail.component.html'
})
export class AssertionDetailComponent implements OnInit {
  assertion: IAssertion;
  orcidBaseUrl: string = ORCID_BASE_URL;
  ownerId: string;

  constructor(protected activatedRoute: ActivatedRoute, protected userService: UserService, protected translate: TranslateService) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ assertion }) => {
      this.assertion = assertion;
      this.userService.find(this.assertion.ownerId).subscribe(user => {
        this.ownerId = user.body.email;
      });
    });
  }

  previousState() {
    window.history.back();
  }

  successMessage() {
    alert(this.translate.instant('gatewayApp.assertionServiceAssertion.copySuccess.string'));
  }
}
