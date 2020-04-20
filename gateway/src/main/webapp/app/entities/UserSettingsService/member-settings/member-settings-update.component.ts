import { Component, OnInit } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IMemberSettings, MemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';
import { MemberSettingsService } from './member-settings.service';
import { AccountService, Account } from 'app/core';

@Component({
  selector: 'jhi-member-settings-update',
  templateUrl: './member-settings-update.component.html'
})
export class MemberSettingsUpdateComponent implements OnInit {
  isSaving: boolean;

  editForm = this.fb.group({
    id: [],
    clientId: [null, [Validators.required]],
    clientName: [],
    salesforceId: [null, [Validators.required]],
    parentSalesforceId: [],
    isConsortiumLead: [null, [Validators.required]],
    assertionServiceEnabled: [],
    createdBy: [],
    createdDate: [],
    lastModifiedBy: [],
    lastModifiedDate: []
  });

  constructor(
    private accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected memberSettingsService: MemberSettingsService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.isSaving = false;
    this.accountService.identity().then((account: Account) => {});
    this.activatedRoute.data.subscribe(({ memberSettings }) => {
      this.updateForm(memberSettings);
    });
  }

  updateForm(memberSettings: IMemberSettings) {
    this.editForm.patchValue({
      id: memberSettings.id,
      clientId: memberSettings.clientId,
      clientName: memberSettings.clientName,
      salesforceId: memberSettings.salesforceId,
      parentSalesforceId: memberSettings.parentSalesforceId,
      isConsortiumLead: memberSettings.isConsortiumLead,
      assertionServiceEnabled: memberSettings.assertionServiceEnabled,
      createdBy: memberSettings.createdBy,
      createdDate: memberSettings.createdDate != null ? memberSettings.createdDate.format(DATE_TIME_FORMAT) : null,
      lastModifiedBy: memberSettings.lastModifiedBy,
      lastModifiedDate: memberSettings.lastModifiedDate != null ? memberSettings.lastModifiedDate.format(DATE_TIME_FORMAT) : null
    });
  }

  previousState() {
    window.history.back();
  }

  save() {
    this.isSaving = true;
    const memberSettings = this.createFromForm();
    if (memberSettings.id !== undefined) {
      this.subscribeToSaveResponse(this.memberSettingsService.update(memberSettings));
    } else {
      this.subscribeToSaveResponse(this.memberSettingsService.create(memberSettings));
    }
  }

  private createFromForm(): IMemberSettings {
    return {
      ...new MemberSettings(),
      id: this.editForm.get(['id']).value,
      clientId: this.editForm.get(['clientId']).value,
      clientName: this.editForm.get(['clientName']).value,
      salesforceId: this.editForm.get(['salesforceId']).value,
      parentSalesforceId: this.editForm.get(['parentSalesforceId']).value,
      isConsortiumLead: this.editForm.get(['isConsortiumLead']).value,
      assertionServiceEnabled: this.editForm.get(['assertionServiceEnabled']).value,
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IMemberSettings>>) {
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
