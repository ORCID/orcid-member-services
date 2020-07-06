import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { faTimesCircle, faCheckCircle } from '@fortawesome/free-solid-svg-icons';

import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';

@Component({
  selector: 'jhi-ms-member-detail',
  templateUrl: './ms-member-detail.component.html'
})
export class MSMemberDetailComponent implements OnInit {
  msMember: IMSMember;
  faTimesCircle = faTimesCircle;
  faCheckCircle = faCheckCircle;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msMember }) => {
      this.msMember = msMember;
    });
  }

  previousState() {
    window.history.back();
  }
}
