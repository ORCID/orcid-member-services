import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, FormControl, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IMSMember, MSMember } from 'app/shared/model/MSUserService/ms-member.model';
import { MSMemberService } from './ms-member.service';
import { AccountService } from 'app/core';
import { BASE_URL, ORCID_BASE_URL } from 'app/app.constants';
import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { JhiAlertService } from 'ng-jhipster';

function consortiumLeadValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: boolean } | null => {
    if (control.value !== undefined && control.parent !== undefined) {
      const isConsortiumLead = control.value;
      if (isConsortiumLead) {
        control.parent.get('parentSalesforceId').disable();
        control.parent.get('parentSalesforceId').setValue(null);
      } else {
        control.parent.get('parentSalesforceId').enable();
      }
    }
    return null;
  };
}

function clientIdValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: boolean } | null => {
    if (control.parent !== undefined && control.value !== undefined && isNaN(control.value)) {
      const clientIdValue = control.value;
      const isConsortiumLead = control.parent.get('isConsortiumLead').value;
      const assertionServiceEnabled = control.parent.get('assertionServiceEnabled').value;
      if (!isConsortiumLead && clientIdValue === '') {
        return Validators.required(control.parent.get('clientId'));
      }
      if (isConsortiumLead && (!clientIdValue || clientIdValue === '')) {
        return null;
      }
      if (!assertionServiceEnabled && (!clientIdValue || clientIdValue === '')) {
        return null;
      }
      if (clientIdValue.startsWith('APP-') && clientIdValue.match(/APP-[A-Z0-9]{16}$/)) {
        return null;
      } else if (clientIdValue.match(/[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/)) {
        return null;
      }
      return { validClientId: false };
    }
    if (control.parent !== undefined) {
      if (control.parent.get('isConsortiumLead').value) {
        return null;
      }
    }
    if (control.parent !== undefined) {
      if (!control.parent.get('assertionServiceEnabled').value) {
        return null;
      }
    }
    return { validClientId: false };
  };
}

@Component({
  selector: 'jhi-ms-member-update',
  templateUrl: './ms-member-update.component.html'
})
export class MSMemberUpdateComponent implements OnInit {
  orcidBaseUrl: string = ORCID_BASE_URL;
  baseUrl: string = BASE_URL;
  isSaving: boolean;
  validation: any;

  editForm = this.fb.group({
    id: [],
    clientId: new FormControl(null, [clientIdValidator()]),
    clientName: [null, [Validators.required]],
    salesforceId: [null, [Validators.required]],
    parentSalesforceId: [],
    isConsortiumLead: [null, [Validators.required, consortiumLeadValidator()]],
    assertionServiceEnabled: [],
    createdBy: [],
    createdDate: [],
    lastModifiedBy: [],
    lastModifiedDate: []
  });

  constructor(
    private accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected msMemberService: MSMemberService,
    private fb: FormBuilder,
    private alertService: JhiAlertService
  ) {
    this.validation = {};
  }

  ngOnInit() {
    this.isSaving = false;
    this.accountService.identity().then((account: IMSUser) => {});
    this.activatedRoute.data.subscribe(({ msMember }) => {
      this.updateForm(msMember);
    });

    this.onChanges();
  }

  onChanges(): void {
    this.editForm.get('isConsortiumLead').valueChanges.subscribe(value => {
      this.editForm.get('parentSalesforceId').updateValueAndValidity();
      this.editForm.get('clientId').markAsTouched();
      this.editForm.get('clientId').updateValueAndValidity();
    });

    this.editForm.get('clientId').valueChanges.subscribe(value => {
      if (!value || (value && value === '')) {
        if (this.editForm.get('assertionServiceEnabled').value) {
          this.editForm.get('assertionServiceEnabled').reset();
          this.editForm.get('clientId').updateValueAndValidity();
        }
        if (!this.editForm.get('assertionServiceEnabled').disabled) {
          this.editForm.get('assertionServiceEnabled').disable();
        }
      } else {
        this.editForm.get('assertionServiceEnabled').enable();
      }
    });
  }

  updateForm(msMember: IMSMember) {
    this.editForm.patchValue({
      id: msMember.id,
      clientId: msMember.clientId,
      clientName: msMember.clientName,
      salesforceId: msMember.salesforceId,
      parentSalesforceId: msMember.parentSalesforceId,
      isConsortiumLead: msMember.isConsortiumLead,
      assertionServiceEnabled: msMember.assertionServiceEnabled ? true : false,
      createdBy: msMember.createdBy,
      createdDate: msMember.createdDate != null ? msMember.createdDate.format(DATE_TIME_FORMAT) : null,
      lastModifiedBy: msMember.lastModifiedBy,
      lastModifiedDate: msMember.lastModifiedDate != null ? msMember.lastModifiedDate.format(DATE_TIME_FORMAT) : null
    });
    const clientId = this.editForm.get('clientId').value;
    if (!clientId || (clientId && clientId === '')) {
      this.editForm.get('assertionServiceEnabled').reset();
      this.editForm.get('assertionServiceEnabled').disable();
    } else {
      this.editForm.get('assertionServiceEnabled').enable();
    }
  }

  navigateToMembersList() {
    this.router.navigate(['/ms-member']);
  }

  save() {
    this.isSaving = true;
    const msMember = this.createFromForm();
    this.msMemberService.validate(msMember).subscribe(response => {
      const data = response.body;
      if (data.valid) {
        if (msMember.id !== undefined) {
          this.subscribeToUpdateResponse(this.msMemberService.update(msMember));
        } else {
          this.subscribeToSaveResponse(this.msMemberService.create(msMember));
        }
      } else {
        this.isSaving = false;
        this.validation = data;
      }
    });
  }

  private createFromForm(): IMSMember {
    return {
      ...new MSMember(),
      id: this.editForm.get(['id']).value,
      clientId: this.editForm.get(['clientId']).value,
      clientName: this.editForm.get(['clientName']).value,
      salesforceId: this.editForm.get(['salesforceId']).value,
      parentSalesforceId: this.editForm.get(['parentSalesforceId']).value,
      isConsortiumLead: this.editForm.get(['isConsortiumLead']).value,
      assertionServiceEnabled: this.editForm.get(['assertionServiceEnabled']).value ? true : false,
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IMSMember>>) {
    result.subscribe(() => this.onSaveSuccess(), () => this.onSaveError());
  }

  protected onSaveSuccess() {
    this.isSaving = false;
    this.navigateToMembersList();
    this.alertService.success('memberServiceApp.member.created.string');
  }

  protected subscribeToUpdateResponse(result: Observable<HttpResponse<IMSMember>>) {
    result.subscribe(() => this.onUpdateSuccess(), () => this.onSaveError());
  }

  protected onUpdateSuccess() {
    this.isSaving = false;
    this.navigateToMembersList();
    this.alertService.success('memberServiceApp.member.updated.string');
  }

  protected onSaveError() {
    this.isSaving = false;
  }
}
