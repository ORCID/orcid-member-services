import { Component, OnInit } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IAffiliation, Affiliation } from 'app/shared/model/AssertionServices/affiliation.model';
import { AffiliationService } from './affiliation.service';

@Component({
  selector: 'jhi-affiliation-update',
  templateUrl: './affiliation-update.component.html'
})
export class AffiliationUpdateComponent implements OnInit {
  isSaving: boolean;

  editForm = this.fb.group({
    id: [],
    email: [null, [Validators.pattern('.*@.*..*'), Validators.required]],
    affiliationSection: [null, [Validators.required]],
    departmentName: [null, [Validators.maxLength(4000)]],
    roleTitle: [null, [Validators.maxLength(4000)]],
    startYear: [],
    startMonth: [],
    startDay: [],
    endYear: [],
    endMonth: [],
    endDay: [],
    orgName: [null, [Validators.required]],
    orgCountry: [null, [Validators.required]],
    orgCity: [null, [Validators.required]],
    orgRegion: [],
    disambiguatedOrgId: [null, [Validators.required]],
    disambiguationSource: [],
    externalId: [],
    externalIdType: [],
    externalIdUrl: [],
    putCode: [],
    created: [],
    modified: [],
    deletedFromORCID: [],
    sent: [],
    adminId: [null, [Validators.required]]
  });

  constructor(protected affiliationService: AffiliationService, protected activatedRoute: ActivatedRoute, private fb: FormBuilder) {}

  ngOnInit() {
    this.isSaving = false;
    this.activatedRoute.data.subscribe(({ affiliation }) => {
      this.updateForm(affiliation);
    });
  }

  updateForm(affiliation: IAffiliation) {
    this.editForm.patchValue({
      id: affiliation.id,
      email: affiliation.email,
      affiliationSection: affiliation.affiliationSection,
      departmentName: affiliation.departmentName,
      roleTitle: affiliation.roleTitle,
      startYear: affiliation.startYear,
      startMonth: affiliation.startMonth,
      startDay: affiliation.startDay,
      endYear: affiliation.endYear,
      endMonth: affiliation.endMonth,
      endDay: affiliation.endDay,
      orgName: affiliation.orgName,
      orgCountry: affiliation.orgCountry,
      orgCity: affiliation.orgCity,
      orgRegion: affiliation.orgRegion,
      disambiguatedOrgId: affiliation.disambiguatedOrgId,
      disambiguationSource: affiliation.disambiguationSource,
      externalId: affiliation.externalId,
      externalIdType: affiliation.externalIdType,
      externalIdUrl: affiliation.externalIdUrl,
      putCode: affiliation.putCode,
      created: affiliation.created != null ? affiliation.created.format(DATE_TIME_FORMAT) : null,
      modified: affiliation.modified != null ? affiliation.modified.format(DATE_TIME_FORMAT) : null,
      deletedFromORCID: affiliation.deletedFromORCID != null ? affiliation.deletedFromORCID.format(DATE_TIME_FORMAT) : null,
      sent: affiliation.sent,
      adminId: affiliation.adminId
    });
  }

  previousState() {
    window.history.back();
  }

  save() {
    this.isSaving = true;
    const affiliation = this.createFromForm();
    if (affiliation.id !== undefined) {
      this.subscribeToSaveResponse(this.affiliationService.update(affiliation));
    } else {
      this.subscribeToSaveResponse(this.affiliationService.create(affiliation));
    }
  }

  private createFromForm(): IAffiliation {
    return {
      ...new Affiliation(),
      id: this.editForm.get(['id']).value,
      email: this.editForm.get(['email']).value,
      affiliationSection: this.editForm.get(['affiliationSection']).value,
      departmentName: this.editForm.get(['departmentName']).value,
      roleTitle: this.editForm.get(['roleTitle']).value,
      startYear: this.editForm.get(['startYear']).value,
      startMonth: this.editForm.get(['startMonth']).value,
      startDay: this.editForm.get(['startDay']).value,
      endYear: this.editForm.get(['endYear']).value,
      endMonth: this.editForm.get(['endMonth']).value,
      endDay: this.editForm.get(['endDay']).value,
      orgName: this.editForm.get(['orgName']).value,
      orgCountry: this.editForm.get(['orgCountry']).value,
      orgCity: this.editForm.get(['orgCity']).value,
      orgRegion: this.editForm.get(['orgRegion']).value,
      disambiguatedOrgId: this.editForm.get(['disambiguatedOrgId']).value,
      disambiguationSource: this.editForm.get(['disambiguationSource']).value,
      externalId: this.editForm.get(['externalId']).value,
      externalIdType: this.editForm.get(['externalIdType']).value,
      externalIdUrl: this.editForm.get(['externalIdUrl']).value,
      putCode: this.editForm.get(['putCode']).value,
      created: this.editForm.get(['created']).value != null ? moment(this.editForm.get(['created']).value, DATE_TIME_FORMAT) : undefined,
      modified: this.editForm.get(['modified']).value != null ? moment(this.editForm.get(['modified']).value, DATE_TIME_FORMAT) : undefined,
      deletedFromORCID:
        this.editForm.get(['deletedFromORCID']).value != null
          ? moment(this.editForm.get(['deletedFromORCID']).value, DATE_TIME_FORMAT)
          : undefined,
      sent: this.editForm.get(['sent']).value,
      adminId: this.editForm.get(['adminId']).value
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IAffiliation>>) {
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
