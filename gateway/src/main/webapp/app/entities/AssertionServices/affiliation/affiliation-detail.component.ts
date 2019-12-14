import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IAffiliation } from 'app/shared/model/AssertionServices/affiliation.model';

@Component({
  selector: 'jhi-affiliation-detail',
  templateUrl: './affiliation-detail.component.html'
})
export class AffiliationDetailComponent implements OnInit {
  affiliation: IAffiliation;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ affiliation }) => {
      this.affiliation = affiliation;
    });
  }

  previousState() {
    window.history.back();
  }
}
