import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';

@Component({
  selector: 'jhi-member-services-user-detail',
  templateUrl: './member-services-user-detail.component.html'
})
export class MemberServicesUserDetailComponent implements OnInit {
  memberServicesUser: IMemberServicesUser;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ memberServicesUser }) => {
      this.memberServicesUser = memberServicesUser;
    });
  }

  previousState() {
    window.history.back();
  }
}
