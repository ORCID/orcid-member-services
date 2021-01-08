import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { JhiAlertService } from 'ng-jhipster';
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons';

import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { IMSUser, MSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { MSUserService } from './ms-user.service';

import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';
import { emailValidator } from 'app/shared/util/app-validators';
import { AccountService } from 'app/core';
import { SERVER_API_URL } from 'app/app.constants';

@Component({
  selector: 'jhi-ms-user-update',
  templateUrl: './ms-user-update.component.html'
})
export class MSUserUpdateComponent implements OnInit {
  isSaving: boolean;
  isExistentMember: boolean;
  existentMSUser: IMSUser;
  faCheckCircle = faCheckCircle;
  showIsAdminCheckbox = false;
  currentAccount: any;

  editForm = this.fb.group({
    id: [],
    login: ['', [Validators.required, Validators.email, Validators.maxLength(50), emailValidator]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    mainContact: [],
    assertionServiceEnabled: [],
    salesforceId: ['', Validators.required],
    activated: [],
    isAdmin: [],
    createdBy: [],
    createdDate: [],
    lastModifiedBy: [],
    lastModifiedDate: []
  });

  membersList = [] as IMSMember[];
  hasOwner = false;

  constructor(
    protected jhiAlertService: JhiAlertService,
    protected msUserService: MSUserService,
    protected msMemberService: MSMemberService,
    protected activatedRoute: ActivatedRoute,
    protected accountService: AccountService,
    private fb: FormBuilder,
    private cdref: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.isSaving = false;
    this.isExistentMember = false;
    this.accountService.identity().then(account => {
      this.currentAccount = account;
    });
    this.activatedRoute.data.subscribe(({ msUser }) => {
      this.existentMSUser = msUser;
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

    this.onChanges();
  }

  onChanges(): void {
    this.editForm.get('salesforceId').valueChanges.subscribe(val => {
      const selectedOrg = this.membersList.find(cm => cm.salesforceId === this.editForm.get(['salesforceId']).value);
      if (this.hasRoleAdmin()) {
        if (selectedOrg) {
          this.showIsAdminCheckbox = selectedOrg.superadminEnabled;
        } else {
          this.showIsAdminCheckbox = false;
        }
      } else {
        this.showIsAdminCheckbox = false;
      }
      this.editForm.patchValue({
        isAdmin: false
      });
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
      activated: msUser.activated,
      isAdmin: msUser.isAdmin,
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
    if (this.existentMSUser.mainContact) {
      return true;
    }
    return this.isExistentMember;
  }

  getSalesForceId() {
    return this.accountService.getSalesforceId();
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN']);
  }

  hasOwnerValidation() {
    this.isSaving = true;
    this.msUserService.hasOwner(this.editForm.get('salesforceId').value).subscribe(value => {
      this.isSaving = false;
      if (!this.editForm.get('mainContact').value) {
        this.hasOwner = false;
      } else {
        this.hasOwner = value;
      }
    });
  }

  save() {
    if (this.editForm.valid) {
      this.isSaving = true;
      const msUser = this.createFromForm();
      if (msUser.id !== undefined) {
        if (this.currentAccount.id === msUser.id) {
          if (this.currentAccount.mainContact !== msUser.mainContact) {
            this.subscribeToSaveResponseWithOwnershipChange(this.msUserService.update(msUser));
          } else {
            this.subscribeToSaveResponse(this.msUserService.update(msUser));
          }
        } else if (msUser.mainContact && !this.hasRoleAdmin()) {
          this.subscribeToSaveResponseWithOwnershipChange(this.msUserService.update(msUser));
        } else {
          this.subscribeToSaveResponse(this.msUserService.update(msUser));
        }
      } else {
        if (msUser.mainContact && !this.hasRoleAdmin()) {
          this.subscribeToSaveResponseWithOwnershipChange(this.msUserService.create(msUser));
        } else {
          this.subscribeToSaveResponse(this.msUserService.create(msUser));
        }
      }
    }
  }

  sendActivate() {
    this.msUserService.sendActivate(this.existentMSUser).subscribe(res => {
      if (res.ok) {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.success', null, null);
      } else {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.error', null, null);
      }
      this.previousState();
    });
  }

  private createFromForm(): IMSUser {
    return {
      ...new MSUser(),
      id: this.editForm.get(['id']).value,
      login: this.editForm.get(['login']).value,
      firstName: this.editForm.get(['firstName']).value,
      lastName: this.editForm.get(['lastName']).value,
      mainContact: this.editForm.get(['mainContact']).value,
      isAdmin: this.editForm.get(['isAdmin']) ? this.editForm.get(['isAdmin']).value : false,
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

  protected subscribeToSaveResponseWithOwnershipChange(result: Observable<HttpResponse<IMSUser>>) {
    result.subscribe(() => this.onSaveSuccessOwnershipChange(), () => this.onSaveError());
  }

  protected onSaveSuccess() {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveSuccessOwnershipChange() {
    this.isSaving = false;
    window.location.href = SERVER_API_URL;
  }

  protected onSaveError() {
    this.isSaving = false;
  }

  protected onError(errorMessage: string) {
    this.jhiAlertService.error(errorMessage, null, null);
  }
}
