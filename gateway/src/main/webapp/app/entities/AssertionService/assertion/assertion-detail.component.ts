import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IAssertion } from 'app/shared/model/AssertionService/assertion.model';
import { ORCID_BASE_URL } from 'app/app.constants';


@Component({
  selector: 'jhi-assertion-detail',
  templateUrl: './assertion-detail.component.html'
})
export class AssertionDetailComponent implements OnInit {
  assertion: IAssertion;
  orcidBaseUrl: string = ORCID_BASE_URL;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ assertion }) => {
      this.assertion = assertion;
    });
  }

  previousState() {
    window.history.back();
  }
}
