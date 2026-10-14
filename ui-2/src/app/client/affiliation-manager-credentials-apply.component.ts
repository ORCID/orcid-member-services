import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { faBan, faEnvelope, faInfoCircle } from '@fortawesome/free-solid-svg-icons'
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap'
import { EMPTY, switchMap } from 'rxjs'
import { AccountService } from '../account'
import { AlertMessage, AlertType } from '../app.constants'
import { isConsortiumMember } from '../member/model/member.model'
import { MemberService } from '../member/service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { AffiliationManagerCredentialsApplication } from './model/affiliation-manager-credentials-application'
import { ApiCredentialsService } from './service/api-credentials.service'
import { websiteUrlValidator } from './validators/url.validators'

type LookedUpField = 'organizationName' | 'consortiumLeadName' | 'institutionHomepageUrl'

@Component({
  selector: 'app-affiliation-manager-credentials-apply',
  templateUrl: './affiliation-manager-credentials-apply.component.html',
  styleUrls: ['./affiliation-manager-credentials-apply.component.scss'],
  imports: [ReactiveFormsModule, FaIconComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AffiliationManagerCredentialsApplyComponent implements OnInit {
  private fb = inject(NonNullableFormBuilder)
  private router = inject(Router)
  private activatedRoute = inject(ActivatedRoute)
  private accountService = inject(AccountService)
  private memberService = inject(MemberService)
  private alertService = inject(AlertService)
  private apiCredentialsService = inject(ApiCredentialsService)
  private destroyRef = inject(DestroyRef)

  protected faBan = faBan
  protected faEnvelope = faEnvelope
  protected faInfoCircle = faInfoCircle

  protected isSaving = signal(false)

  // Looked-up fields are read-only unless the lookup returned nothing, then the member fills them in.
  protected editableFields = signal<ReadonlySet<LookedUpField>>(new Set())

  // The requester email comes from the signed-in account and is always read-only. The other
  // looked-up fields stay required so an empty value must be filled in before submitting.
  // Notes is optional.
  applyForm = this.fb.group({
    organizationName: this.fb.control('', [Validators.required]),
    consortiumLeadName: this.fb.control('', [Validators.required]),
    requesterEmail: this.fb.control('', [Validators.required]),
    institutionHomepageUrl: this.fb.control('', [Validators.required, websiteUrlValidator()]),
    notes: this.fb.control(''),
  })

  private collectionRoute(): string[] {
    const memberId = this.activatedRoute.parent?.snapshot.paramMap.get('memberId')
    return memberId ? ['/api-credentials', memberId] : ['/']
  }

  ngOnInit(): void {
    this.accountService.accountData
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((account) => this.applyForm.controls.requesterEmail.setValue(account?.email ?? ''))

    const memberId = this.activatedRoute.parent?.snapshot.paramMap.get('memberId')
    if (!memberId) {
      this.router.navigate(['/'])
      return
    }

    this.memberService
      .find(memberId)
      .pipe(
        switchMap((member) => {
          // The form is only offered to consortium members; send anyone else back to the list.
          if (!isConsortiumMember(member)) {
            this.router.navigate(this.collectionRoute())
            return EMPTY
          }
          this.applyLoaded('organizationName', member.clientName)
          return this.memberService.find(member.parentSalesforceId)
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (consortiumLead) => this.applyLoaded('consortiumLeadName', consortiumLead.clientName),
        error: () => {
          this.makeEditableIfEmpty('organizationName')
          this.makeEditableIfEmpty('consortiumLeadName')
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_MEMBER_LOAD_ERROR)
        },
      })

    // getMemberData is cached: undefined while loading, null on failure, otherwise the member data.
    this.memberService
      .getMemberData(memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((memberData) => {
        if (memberData !== undefined) {
          this.applyLoaded('institutionHomepageUrl', memberData?.website)
        }
      })
  }

  private applyLoaded(field: LookedUpField, value: string | null | undefined) {
    const control = this.applyForm.controls[field]
    if (control.dirty) {
      return
    }
    const text = value ?? ''
    control.setValue(text)
    // an empty or invalid looked-up value must be correctable by the member
    const needsInput = !text || control.invalid
    this.editableFields.update((fields) => {
      const next = new Set(fields)
      if (needsInput) {
        next.add(field)
      } else {
        next.delete(field)
      }
      return next
    })
  }

  private makeEditableIfEmpty(field: LookedUpField) {
    if (!this.applyForm.controls[field].value) {
      this.editableFields.update((fields) => new Set(fields).add(field))
    }
  }

  save() {
    if (this.isSaving()) {
      return
    }
    if (this.applyForm.invalid) {
      this.applyForm.markAllAsTouched()
      return
    }
    const value = this.applyForm.getRawValue()
    const notes = value.notes.trim()
    const payload: AffiliationManagerCredentialsApplication = {
      orgName: value.organizationName.trim(),
      consortiumLeadName: value.consortiumLeadName.trim(),
      orgHomePage: value.institutionHomepageUrl.trim(),
      ...(notes ? { notes } : {}),
    }

    this.isSaving.set(true)
    this.apiCredentialsService
      .submitAffiliationManagerCredentialsApplication(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSaving.set(false)
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_APPLICATION_SUBMITTED)
          this.router.navigate(this.collectionRoute())
        },
        error: () => {
          this.isSaving.set(false)
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_APPLICATION_ERROR)
        },
      })
  }

  cancel() {
    this.router.navigate(this.collectionRoute())
  }
}
