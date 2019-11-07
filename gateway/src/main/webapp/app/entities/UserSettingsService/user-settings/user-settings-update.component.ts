import { Component, OnInit } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IUserSettings, UserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';
import { UserSettingsService } from './user-settings.service';

@Component({
  selector: 'jhi-user-settings-update',
  templateUrl: './user-settings-update.component.html'
})
export class UserSettingsUpdateComponent implements OnInit {
  isSaving: boolean;

  editForm = this.fb.group({
    id: [],
    login: [],
    salesforceId: [],
    assertionsServiceDisabled: [],
    mainContact: [],
    createdBy: [],
    createdDate: [],
    lastModifiedBy: [],
    lastModifiedDate: []
  });

  constructor(protected userSettingsService: UserSettingsService, protected activatedRoute: ActivatedRoute, private fb: FormBuilder) {}

  ngOnInit() {
    this.isSaving = false;
    this.activatedRoute.data.subscribe(({ userSettings }) => {
      this.updateForm(userSettings);
    });
  }

  updateForm(userSettings: IUserSettings) {
    this.editForm.patchValue({
      id: userSettings.id,
      login: userSettings.login,
      salesforceId: userSettings.salesforceId,
      assertionsServiceDisabled: userSettings.assertionsServiceDisabled,
      mainContact: userSettings.mainContact,
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
      salesforceId: this.editForm.get(['salesforceId']).value,
      assertionsServiceDisabled: this.editForm.get(['assertionsServiceDisabled']).value,
      mainContact: this.editForm.get(['mainContact']).value,
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
}
