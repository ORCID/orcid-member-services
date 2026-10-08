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

  // The member-service collates the organisation, consortium lead, requester and homepage
  // itself when sending the email — these fields are read-only in the template — but they're
  // still real form controls so a failed lookup (e.g. no website on file) is flagged as a
  // required field instead of being submitted silently as blank. Notes is the only field the
  // member actually types, and it's optional.
  applyForm = this.fb.group({
    organizationName: this.fb.control('', [Validators.required]),
    consortiumLeadName: this.fb.control('', [Validators.required]),
    requesterEmail: this.fb.control('', [Validators.required]),
    institutionHomepageUrl: this.fb.control('', [Validators.required]),
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
          this.applyForm.controls.organizationName.setValue(member.clientName ?? '')
          return this.memberService.find(member.parentSalesforceId)
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (consortiumLead) => this.applyForm.controls.consortiumLeadName.setValue(consortiumLead.clientName ?? ''),
        error: () => this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_MEMBER_LOAD_ERROR),
      })

    // The homepage is the member's Salesforce website. getMemberData is cached and emits
    // undefined/null while loading or on failure, so fall back to an empty string.
    this.memberService
      .getMemberData(memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((memberData) => this.applyForm.controls.institutionHomepageUrl.setValue(memberData?.website ?? ''))
  }

  save() {
    if (this.isSaving()) {
      return
    }
    if (this.applyForm.invalid) {
      this.applyForm.markAllAsTouched()
      return
    }
    const notes = this.applyForm.getRawValue().notes.trim()
    const payload: AffiliationManagerCredentialsApplication = notes ? { notes } : {}

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
