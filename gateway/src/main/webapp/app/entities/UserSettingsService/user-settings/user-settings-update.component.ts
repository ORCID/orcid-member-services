import { Component, OnInit } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { JhiAlertService } from 'ng-jhipster';

import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IUserSettings, UserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';
import { UserSettingsService } from './user-settings.service';

import { IMemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';
import { MemberSettingsService } from 'app/entities/UserSettingsService/member-settings/member-settings.service';

@Component({
  selector: 'jhi-user-settings-update',
  templateUrl: './user-settings-update.component.html'
})
export class UserSettingsUpdateComponent implements OnInit {
  isSaving: boolean;

  editForm = this.fb.group({
    id: [],
    login: ['', Validators.required],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    mainContact: [],
    assertionServiceEnabled: [],
    salesforceId: [],
    createdBy: [],
    createdDate: [],
    lastModifiedBy: [],
    lastModifiedDate: []
  });

  membersList: IMemberSettings;

  constructor(protected jhiAlertService: JhiAlertService, protected userSettingsService: UserSettingsService, protected memberSettingsService: MemberSettingsService, protected activatedRoute: ActivatedRoute, private fb: FormBuilder) {}

  ngOnInit() {
    this.isSaving = false;
    this.activatedRoute.data.subscribe(({ userSettings }) => {
      this.updateForm(userSettings);
    });
    this.membersList = this.memberSettingsService.getOrgNameMap();
  }

  updateForm(userSettings: IUserSettings) {
    this.editForm.patchValue({
      id: userSettings.id,
      login: userSettings.login,
      password: userSettings.password,
      firstName: userSettings.firstName,
      lastName: userSettings.lastName,
      mainContact: userSettings.mainContact,
      assertionServiceEnabled: userSettings.assertionServiceEnabled,
      salesforceId: userSettings.salesforceId,      
      createdBy: userSettings.createdBy,
      createdDate: userSettings.createdDate != null ? userSettings.createdDate.format(DATE_TIME_FORMAT) : null,
      lastModifiedBy: userSettings.lastModifiedBy,
      lastModifiedDate: userSettings.lastModifiedDate != null ? userSettings.lastModifiedDate.format(DATE_TIME_FORMAT) : null
    });
  }

  previousState() {
    window.history.back();
  }

  save() {
    this.isSaving = true;
    const userSettings = this.createFromForm();    
    if (userSettings.id !== undefined) {
      this.subscribeToSaveResponse(this.userSettingsService.update(userSettings));
    } else {
      this.subscribeToSaveResponse(this.userSettingsService.create(userSettings));
    }
  }

  private createFromForm(): IUserSettings {
    return {
      ...new UserSettings(),
      id: this.editForm.get(['id']).value,
      login: this.editForm.get(['login']).value,
      password: this.editForm.get(['password']).value,
      firstName: this.editForm.get(['firstName']).value,
      lastName: this.editForm.get(['lastName']).value,
      mainContact: this.editForm.get(['mainContact']).value,
      assertionServiceEnabled: this.editForm.get(['assertionServiceEnabled']).value,
      salesforceId: this.editForm.get(['salesforceId']).value,      
      createdBy: this.editForm.get(['createdBy']).value,
      createdDate:
        this.editForm.get(['createdDate']).value != null ? moment(this.editForm.get(['createdDate']).value, DATE_TIME_FORMAT) : undefined,
      lastModifiedBy: this.editForm.get(['lastModifiedBy']).value,
      lastModifiedDate:
        this.editForm.get(['lastModifiedDate']).value != null
          ? moment(this.editForm.get(['lastModifiedDate']).value, DATE_TIME_FORMAT)
          : undefined
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IUserSettings>>) {
    result.subscribe(() => this.onSaveSuccess(), () => this.onSaveError());
  }

  protected onSaveSuccess() {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError() {
    this.isSaving = false;
  }

  protected onError(errorMessage: string) {
    this.jhiAlertService.error(errorMessage, null, null);
  }
}
