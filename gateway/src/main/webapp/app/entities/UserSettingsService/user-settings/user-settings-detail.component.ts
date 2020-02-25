import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IUserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';

import { IMemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';
import { MemberSettingsService } from 'app/entities/UserSettingsService/member-settings/member-settings.service';

@Component({
  selector: 'jhi-user-settings-detail',
  templateUrl: './user-settings-detail.component.html'
})
export class UserSettingsDetailComponent implements OnInit {
  userSettings: IUserSettings;
  membersList: any;

  constructor(protected activatedRoute: ActivatedRoute, protected memberSettingsService: MemberSettingsService) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ userSettings }) => {
      this.userSettings = userSettings;
    });
    this.memberSettingsService.allMembers$
      .subscribe(
        (res: HttpResponse<IMemberSettings[]>) => {
          this.membersList = res;
          this.membersList = Array.of(this.membersList);
        },
        (res: HttpErrorResponse) => {
          return this.onError(res.message);
        };
      )
  }

  getOrgName(id: string) {
    if(this.membersList){
      for (const member of this.membersList[0].body) {
        if (id === member.salesforceId) {
          return member.clientName;
        }
      }
    }
  }

  previousState() {
    window.history.back();
  }
}
