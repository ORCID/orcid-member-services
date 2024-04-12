import { Component, OnInit } from '@angular/core'
import { AbstractControl, FormBuilder, FormControl, ValidatorFn, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { Observable } from 'rxjs'
import * as moment from 'moment'
import { MemberService } from './service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { AlertType, BASE_URL, DATE_TIME_FORMAT, ORCID_BASE_URL } from '../app.constants'
import { IMember, Member } from './model/member.model'
import { faBan, faSave } from '@fortawesome/free-solid-svg-icons'

function parentSalesforceIdValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: boolean } | null => {
    if (control.parent && control.value && isNaN(control.value)) {
      const parentSalesforceId = control.value
      const isConsortiumLead = control.parent?.get('isConsortiumLead')?.value
      const salesforceId = control.parent?.get('salesforceId')?.value

      if (isConsortiumLead && parentSalesforceId !== salesforceId) {
        return { validParentSalesforceIdValue: false }
      }
    }
    return null
  }
}

function clientIdValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: boolean } | null => {
    if (control.parent && control.value && isNaN(control.value)) {
      const clientIdValue = control.value
      const isConsortiumLead = control.parent?.get('isConsortiumLead')?.value
      const assertionServiceEnabled = control.parent?.get('assertionServiceEnabled')?.value
      if (!isConsortiumLead && clientIdValue === '') {
        const clientIdControl = control.parent?.get('clientId')
        if (clientIdControl) {
          return Validators.required(clientIdControl)
        }
      }
      if (isConsortiumLead && (!clientIdValue || clientIdValue === '')) {
        return null
      }
      if (!assertionServiceEnabled && (!clientIdValue || clientIdValue === '')) {
        return null
      }
      if (clientIdValue.startsWith('APP-') && clientIdValue.match(/APP-[A-Z0-9]{16}$/)) {
        return null
      } else if (clientIdValue.match(/[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/)) {
        return null
      }
      return { validClientId: false }
    }
    if (control.parent) {
      if (control.parent?.get('isConsortiumLead')?.value) {
        return null
      }
    }
    if (control.parent) {
      if (!control.parent?.get('assertionServiceEnabled')?.value) {
        return null
      }
    }
    return { validClientId: false }
  }
}

@Component({
  selector: 'app-member-update',
  templateUrl: './member-update.component.html',
})
export class MemberUpdateComponent implements OnInit {
  orcidBaseUrl: string = ORCID_BASE_URL
  baseUrl: string = BASE_URL
  isSaving = false
  validation: any
  faBan = faBan
  faSave = faSave

  editForm = this.fb.group({
    id: new FormControl<string | null>(null),
    clientId: new FormControl<string | null>(null, [clientIdValidator()]),
    clientName: new FormControl<string | null>(null, [Validators.required]),
    salesforceId: new FormControl<string | null>(null, [Validators.required]),
    parentSalesforceId: new FormControl<string | null>(null, [parentSalesforceIdValidator()]),
    isConsortiumLead: new FormControl<boolean | null>(null, [Validators.required]),
    assertionServiceEnabled: new FormControl<boolean | null>(false),
    createdBy: new FormControl<string | null>(null),
    createdDate: new FormControl<string | null>(null),
    lastModifiedBy: new FormControl<string | null>(null),
    lastModifiedDate: new FormControl<string | null>(null),
  })

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected memberService: MemberService,
    private fb: FormBuilder,
    private alertService: AlertService
  ) {
    this.validation = {}
  }

  ngOnInit() {
    this.isSaving = false
    this.activatedRoute.data.subscribe(({ member }) => {
      this.updateForm(member)
    })

    this.onChanges()
  }

  onChanges(): void {
    this.editForm.get('isConsortiumLead')?.valueChanges.subscribe((value) => {
      this.editForm.get('parentSalesforceId')?.updateValueAndValidity()
      this.editForm.get('clientId')?.markAsTouched()
      this.editForm.get('clientId')?.updateValueAndValidity()
    })

    this.editForm.get('clientId')?.valueChanges.subscribe((value) => {
      if (!value || (value && value === '')) {
        if (this.editForm.get('assertionServiceEnabled')?.value) {
          this.editForm.get('assertionServiceEnabled')?.reset()
          this.editForm.get('clientId')?.updateValueAndValidity()
        }
        if (!this.editForm.get('assertionServiceEnabled')?.disabled) {
          this.editForm.get('assertionServiceEnabled')?.disable()
        }
      } else {
        this.editForm.get('assertionServiceEnabled')?.enable()
      }
    })
  }

  updateForm(member: IMember) {
    this.editForm.patchValue({
      id: member.id,
      clientId: member.clientId,
      clientName: member.clientName,
      salesforceId: member.salesforceId,
      parentSalesforceId: member.parentSalesforceId,
      isConsortiumLead: member.isConsortiumLead,
      assertionServiceEnabled: member.assertionServiceEnabled ? true : false,
      createdBy: member.createdBy,
      createdDate: member.createdDate != null ? member.createdDate.format(DATE_TIME_FORMAT) : null,
      lastModifiedBy: member.lastModifiedBy,
      lastModifiedDate: member.lastModifiedDate != null ? member.lastModifiedDate.format(DATE_TIME_FORMAT) : null,
    })
    const clientId = this.editForm.get('clientId')?.value
    if (!clientId || (clientId && clientId === '')) {
      this.editForm.get('assertionServiceEnabled')?.reset()
      this.editForm.get('assertionServiceEnabled')?.disable()
    } else {
      this.editForm.get('assertionServiceEnabled')?.enable()
    }
  }

  navigateToMembersList() {
    this.router.navigate(['/members'])
  }

  save() {
    this.isSaving = true
    const member = this.createFromForm()
    this.memberService.validate(member).subscribe((data) => {
      if (data.valid) {
        if (member.id !== null) {
          this.subscribeToUpdateResponse(this.memberService.update(member))
        } else {
          this.subscribeToSaveResponse(this.memberService.create(member))
        }
      } else {
        this.isSaving = false
        this.validation = data
      }
    })
  }

  private createFromForm(): IMember {
    return {
      ...new Member(),
      id: this.editForm.get(['id'])?.value || null,
      clientId: this.editForm.get(['clientId'])?.value || null,
      clientName: this.editForm.get(['clientName'])?.value || null,
      salesforceId: this.editForm.get(['salesforceId'])?.value || null,
      parentSalesforceId: this.editForm.get(['parentSalesforceId'])?.value || null,
      isConsortiumLead: this.editForm.get(['isConsortiumLead'])?.value || false,
      assertionServiceEnabled: this.editForm.get(['assertionServiceEnabled'])?.value ? true : false,
      createdBy: this.editForm.get(['createdBy'])?.value,
      createdDate:
        this.editForm.get(['createdDate'])?.value != null
          ? moment(this.editForm.get(['createdDate'])?.value, DATE_TIME_FORMAT)
          : null,
      lastModifiedBy: this.editForm.get(['lastModifiedBy'])?.value,
      lastModifiedDate:
        this.editForm.get(['lastModifiedDate'])?.value != null
          ? moment(this.editForm.get(['lastModifiedDate'])?.value, DATE_TIME_FORMAT)
          : null,
    }
  }

  protected subscribeToSaveResponse(result: Observable<IMember>) {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    })
  }

  protected onSaveSuccess() {
    this.isSaving = false
    this.navigateToMembersList()
    this.alertService.broadcast(AlertType.MEMBER_CREATED)
  }

  protected subscribeToUpdateResponse(result: Observable<IMember>) {
    result.subscribe({
      next: () => this.onUpdateSuccess(),
      error: () => this.onSaveError(),
    })
  }

  protected onUpdateSuccess() {
    this.isSaving = false
    this.navigateToMembersList()
    this.alertService.broadcast(AlertType.MEMBER_UPDATED)
  }

  protected onSaveError() {
    this.isSaving = false
  }
}
