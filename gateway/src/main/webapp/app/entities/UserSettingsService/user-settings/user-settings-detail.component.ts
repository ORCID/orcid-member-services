import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IUserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';

@Component({
  selector: 'jhi-user-settings-detail',
  templateUrl: './user-settings-detail.component.html'
})
export class UserSettingsDetailComponent implements OnInit {
  userSettings: IUserSettings;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ userSettings }) => {
      this.userSettings = userSettings;
    });
  }

  previousState() {
    window.history.back();
  }
}
