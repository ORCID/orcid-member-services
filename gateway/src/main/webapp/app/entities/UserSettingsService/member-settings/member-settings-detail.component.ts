import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';

@Component({
  selector: 'jhi-member-settings-detail',
  templateUrl: './member-settings-detail.component.html'
})
export class MemberSettingsDetailComponent implements OnInit {
  memberSettings: IMemberSettings;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ memberSettings }) => {
      this.memberSettings = memberSettings;
    });
  }

  previousState() {
    window.history.back();
  }
}
