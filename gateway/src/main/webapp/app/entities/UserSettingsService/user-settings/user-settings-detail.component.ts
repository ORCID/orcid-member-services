import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IUserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';
import { MemberSettingsService } from 'app/entities/UserSettingsService/member-settings/member-settings.service';

@Component({
  selector: 'jhi-user-settings-detail',
  templateUrl: './user-settings-detail.component.html'
})
export class UserSettingsDetailComponent implements OnInit {
  userSettings: IUserSettings;

  constructor(protected activatedRoute: ActivatedRoute, protected memberSettingsService: MemberSettingsService) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ userSettings }) => {
      this.userSettings = userSettings;
    });
    this.memberSettingsService.getOrgNameMap();
  }


  previousState() {
    window.history.back();
  }
}
