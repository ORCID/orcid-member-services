import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormBuilder, FormControl, Validators, ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { Observable } from 'rxjs'
import moment from 'moment'
import { MemberService } from './service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { AlertMessage, AlertType, BASE_URL, DATE_TIME_FORMAT, ORCID_BASE_URL } from '../app.constants'
import { IMember, Member } from './model/member.model'
import { faBan, faSave } from '@fortawesome/free-solid-svg-icons'
import {
  clientIdValidator,
  parentSalesforceIdValidator,
  salesforceIdFormatValidator,
} from './validators/member.validators'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-member-update',
  templateUrl: './member-update.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ErrorAlertComponent, NgbAlertModule, FaIconComponent],
})
export class MemberUpdateComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected memberService = inject(MemberService)
  private fb = inject(FormBuilder)
  private alertService = inject(AlertService)
  private destroyRef = inject(DestroyRef)

  protected orcidBaseUrl: string = ORCID_BASE_URL
  protected baseUrl: string = BASE_URL
  protected isSaving = signal(false)
  protected validation: any
  protected faBan = faBan
  protected faSave = faSave

  editForm = this.fb.group({
    id: new FormControl<string | null>(null),
    clientId: new FormControl<string | null>(null, [clientIdValidator()]),
    clientName: new FormControl<string | null>(null, [Validators.required]),
    salesforceId: new FormControl<string | null>(null, [Validators.required, salesforceIdFormatValidator()]),
    parentSalesforceId: new FormControl<string | null>(null, [
      salesforceIdFormatValidator(),
      parentSalesforceIdValidator(),
    ]),
    isConsortiumLead: new FormControl<boolean | null>(null, [Validators.required]),
    assertionServiceEnabled: new FormControl<boolean | null>(false),
    createdBy: new FormControl<string | null>(null),
    createdDate: new FormControl<string | null>(null),
    lastModifiedBy: new FormControl<string | null>(null),
    lastModifiedDate: new FormControl<string | null>(null),
  })

  constructor() {
    this.validation = {}
  }

  ngOnInit() {
    this.isSaving.set(false)
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ member }) => {
      this.updateForm(member)
    })

    this.onChanges()
  }

  onChanges(): void {
    this.editForm.get('isConsortiumLead')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
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
    const sfId = this.editForm.get('salesforceId')?.value

    if (!clientId || (clientId && clientId === '')) {
      this.editForm.get('assertionServiceEnabled')?.reset()
      this.editForm.get('assertionServiceEnabled')?.disable()
    } else {
      this.editForm.get('assertionServiceEnabled')?.enable()
    }

    if (sfId) {
      this.editForm.get('salesforceId')?.disable()
      this.editForm.get('clientName')?.disable()
    }
  }

  navigateToMembersList() {
    this.router.navigate(['/members'])
  }

  save() {
    this.isSaving.set(true)
    const member = this.createFromForm()
    this.memberService.validate(member).subscribe((data) => {
      if (data.valid) {
        if (member.id !== null) {
          this.subscribeToUpdateResponse(this.memberService.update(member))
        } else {
          this.subscribeToSaveResponse(this.memberService.create(member))
        }
      } else {
        this.isSaving.set(false)
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
    this.isSaving.set(false)
    this.navigateToMembersList()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.MEMBER_CREATED)
  }

  protected subscribeToUpdateResponse(result: Observable<IMember>) {
    result.subscribe({
      next: () => this.onUpdateSuccess(),
      error: () => this.onSaveError(),
    })
  }

  protected onUpdateSuccess() {
    this.isSaving.set(false)
    this.navigateToMembersList()
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.MEMBER_UPDATED)
  }

  protected onSaveError() {
    this.isSaving.set(false)
  }
}
