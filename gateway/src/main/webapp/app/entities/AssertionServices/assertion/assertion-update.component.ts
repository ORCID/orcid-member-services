import { Component, OnInit } from '@angular/core';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IAssertion, Assertion } from 'app/shared/model/AssertionServices/assertion.model';
import { AssertionService } from './assertion.service';
import { AFFILIATION_TYPES, COUNTRIES, ORG_ID_TYPES } from 'app/shared/constants/orcid-api.constants';

@Component({
  selector: 'jhi-assertion-update',
  templateUrl: './assertion-update.component.html'
})
export class AssertionUpdateComponent implements OnInit {
  AFFILIATION_TYPES = AFFILIATION_TYPES;
  COUNTRIES = COUNTRIES;
  ORG_ID_TYPES = ORG_ID_TYPES;
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

  constructor(protected assertionService: AssertionService, protected activatedRoute: ActivatedRoute, private fb: FormBuilder) {}

  ngOnInit() {
    this.isSaving = false;
    this.activatedRoute.data.subscribe(({ assertion }) => {
      this.updateForm(assertion);
    });
  }

  updateForm(assertion: IAssertion) {
    this.editForm.patchValue({
      id: assertion.id,
      email: assertion.email,
      affiliationSection: assertion.affiliationSection,
      departmentName: assertion.departmentName,
      roleTitle: assertion.roleTitle,
      startYear: assertion.startYear,
      startMonth: assertion.startMonth,
      startDay: assertion.startDay,
      endYear: assertion.endYear,
      endMonth: assertion.endMonth,
      endDay: assertion.endDay,
      orgName: assertion.orgName,
      orgCountry: assertion.orgCountry,
      orgCity: assertion.orgCity,
      orgRegion: assertion.orgRegion,
      disambiguatedOrgId: assertion.disambiguatedOrgId,
      disambiguationSource: assertion.disambiguationSource,
      externalId: assertion.externalId,
      externalIdType: assertion.externalIdType,
      externalIdUrl: assertion.externalIdUrl,
      putCode: assertion.putCode,
      created: assertion.created != null ? assertion.created.format(DATE_TIME_FORMAT) : null,
      modified: assertion.modified != null ? assertion.modified.format(DATE_TIME_FORMAT) : null,
      deletedFromORCID: assertion.deletedFromORCID != null ? assertion.deletedFromORCID.format(DATE_TIME_FORMAT) : null,
      sent: assertion.sent,
      adminId: assertion.adminId
    });
  }

  previousState() {
    window.history.back();
  }

  save() {
    this.isSaving = true;
    const assertion = this.createFromForm();
    if (assertion.id !== undefined) {
      this.subscribeToSaveResponse(this.assertionService.update(assertion));
    } else {
      this.subscribeToSaveResponse(this.assertionService.create(assertion));
    }
  }

  private createFromForm(): IAssertion {
    return {
      ...new Assertion(),
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IAssertion>>) {
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
