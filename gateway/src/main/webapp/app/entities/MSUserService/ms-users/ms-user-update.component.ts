import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { JhiAlertService } from 'ng-jhipster';

import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IMSUser, MSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { MSUserService } from './ms-user.service';

import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';
import { emailValidator } from 'app/shared/util/app-validators';

@Component({
  selector: 'jhi-ms-user-update',
  templateUrl: './ms-user-update.component.html'
})
export class MSUserUpdateComponent implements OnInit {
  isSaving: boolean;
  isExistentMember: boolean;

  editForm = this.fb.group({
    id: [],
    login: ['', [Validators.required, Validators.email, Validators.maxLength(50), emailValidator]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    mainContact: [],
    assertionServiceEnabled: [],
    salesforceId: ['', Validators.required],
    createdBy: [],
    createdDate: [],
    lastModifiedBy: [],
    lastModifiedDate: []
  });

  membersList = [] as IMSMember[];

  constructor(
    protected jhiAlertService: JhiAlertService,
    protected msUserService: MSUserService,
    protected msMemberService: MSMemberService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder,
    private cdref: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.isSaving = false;
    this.isExistentMember = false;
    this.activatedRoute.data.subscribe(({ msUser }) => {
      this.updateForm(msUser);
    });
    this.editForm.disable();

    this.msMemberService.allMembers$.subscribe(res => {
      if (res.body) {
        this.membersList = [];
        res.body.forEach((msMember: IMSMember) => {
          this.membersList.push(msMember);
        });
        if (this.membersList.length > 0) {
          this.editForm.enable();
          this.cdref.detectChanges();
        }
      }
    });
  }

  updateForm(msUser: IMSUser) {
    this.editForm.patchValue({
      id: msUser.id,
      login: msUser.login,
      firstName: msUser.firstName,
      lastName: msUser.lastName,
      mainContact: msUser.mainContact,
      salesforceId: msUser.salesforceId,
      createdBy: msUser.createdBy,
      createdDate: msUser.createdDate != null ? msUser.createdDate.format(DATE_TIME_FORMAT) : null,
      lastModifiedBy: msUser.lastModifiedBy,
      lastModifiedDate: msUser.lastModifiedDate != null ? msUser.lastModifiedDate.format(DATE_TIME_FORMAT) : null
    });
    if (msUser.salesforceId) {
      this.isExistentMember = true;
    }
  }

  previousState() {
    window.history.back();
  }

  disableSalesForceIdDD() {
    console.log('!!!!! existent member: ', this.isExistentMember);
    return this.isExistentMember;
  }

  save() {
    if (this.editForm.valid) {
      this.isSaving = true;
      const msUser = this.createFromForm();
      if (msUser.id !== undefined) {
        this.subscribeToSaveResponse(this.msUserService.update(msUser));
      } else {
        this.subscribeToSaveResponse(this.msUserService.create(msUser));
      }
    }
  }

  private createFromForm(): IMSUser {
    return {
      ...new MSUser(),
      id: this.editForm.get(['id']).value,
      login: this.editForm.get(['login']).value,
      firstName: this.editForm.get(['firstName']).value,
      lastName: this.editForm.get(['lastName']).value,
      mainContact: this.editForm.get(['mainContact']).value,
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IMSUser>>) {
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
