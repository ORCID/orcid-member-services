import { Component, OnInit } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { IMemberServicesUser, MemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';
import { MemberServicesUserService } from './member-services-user.service';

@Component({
  selector: 'jhi-member-services-user-update',
  templateUrl: './member-services-user-update.component.html'
})
export class MemberServicesUserUpdateComponent implements OnInit {
  isSaving: boolean;

  editForm = this.fb.group({
    id: [],
    user_id: [null, [Validators.required]],
    salesforceId: [],
    parentSalesforceId: [],
    disabled: [],
    mainContact: [],
    assertionServiceEnabled: [],
    oboClientId: []
  });

  constructor(
    protected memberServicesUserService: MemberServicesUserService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.isSaving = false;
    this.activatedRoute.data.subscribe(({ memberServicesUser }) => {
      this.updateForm(memberServicesUser);
    });
  }

  updateForm(memberServicesUser: IMemberServicesUser) {
    this.editForm.patchValue({
      id: memberServicesUser.id,
      user_id: memberServicesUser.user_id,
      salesforceId: memberServicesUser.salesforceId,
      parentSalesforceId: memberServicesUser.parentSalesforceId,
      disabled: memberServicesUser.disabled,
      mainContact: memberServicesUser.mainContact,
      assertionServiceEnabled: memberServicesUser.assertionServiceEnabled,
      oboClientId: memberServicesUser.oboClientId
    });
  }

  previousState() {
    window.history.back();
  }

  save() {
    this.isSaving = true;
    const memberServicesUser = this.createFromForm();
    if (memberServicesUser.id !== undefined) {
      this.subscribeToSaveResponse(this.memberServicesUserService.update(memberServicesUser));
    } else {
      this.subscribeToSaveResponse(this.memberServicesUserService.create(memberServicesUser));
    }
  }

  private createFromForm(): IMemberServicesUser {
    return {
      ...new MemberServicesUser(),
      id: this.editForm.get(['id']).value,
      user_id: this.editForm.get(['user_id']).value,
      salesforceId: this.editForm.get(['salesforceId']).value,
      parentSalesforceId: this.editForm.get(['parentSalesforceId']).value,
      disabled: this.editForm.get(['disabled']).value,
      mainContact: this.editForm.get(['mainContact']).value,
      assertionServiceEnabled: this.editForm.get(['assertionServiceEnabled']).value,
      oboClientId: this.editForm.get(['oboClientId']).value
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IMemberServicesUser>>) {
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
