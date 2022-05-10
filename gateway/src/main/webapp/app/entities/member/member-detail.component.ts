import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { faTimesCircle, faCheckCircle } from '@fortawesome/free-solid-svg-icons';

import { IMSMember } from 'app/shared/model/member.model';

@Component({
  selector: 'jhi-ms-member-detail',
  templateUrl: './member-detail.component.html'
})
export class MSMemberDetailComponent implements OnInit {
  msMember: IMSMember;
  faTimesCircle = faTimesCircle;
  faCheckCircle = faCheckCircle;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msMember }) => {
      this.msMember = msMember;
      if (!this.msMember.type) {
        this.msMember.type = 'unset';
      }
      if (!this.msMember.status) {
        this.msMember.status = 'unset';
      }
    });
  }

  previousState() {
    window.history.back();
  }
}
