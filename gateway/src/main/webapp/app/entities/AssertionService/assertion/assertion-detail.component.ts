import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IAssertion } from 'app/shared/model/AssertionService/assertion.model';

@Component({
  selector: 'jhi-assertion-detail',
  templateUrl: './assertion-detail.component.html'
})
export class AssertionDetailComponent implements OnInit {
  assertion: IAssertion;

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
