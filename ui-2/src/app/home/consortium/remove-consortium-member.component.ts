import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms'
import { Router, ActivatedRoute, RouterLink } from '@angular/router'
import { AccountService } from 'src/app/account'
import { AlertType } from 'src/app/app.constants'
import {
  ISFMemberData,
  SFConsortiumMemberData,
  ISFConsortiumMemberData,
} from 'src/app/member/model/salesforce-member-data.model'
import { MemberService } from 'src/app/member/service/member.service'
import { AlertService } from 'src/app/shared/service/alert.service'
import { DateUtilService } from 'src/app/shared/service/date-util.service'

@Component({
  selector: 'app-remove-consortium-member',
  templateUrl: './remove-consortium-member.component.html',
  styleUrls: ['./remove-consortium-member.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ReactiveFormsModule],
})
export class RemoveConsortiumMemberComponent implements OnInit {
  private memberService = inject(MemberService)
  private accountService = inject(AccountService)
  private fb = inject(FormBuilder)
  private alertService = inject(AlertService)
  private router = inject(Router)
  private dateUtilService = inject(DateUtilService)
  protected activatedRoute = inject(ActivatedRoute)
  private destroyRef = inject(DestroyRef)

  protected memberData = signal<ISFMemberData | undefined | null>(null)
  protected consortiumMember = signal<SFConsortiumMemberData | undefined>(undefined)
  protected isSaving = signal(false)
  protected invalidForm = signal(false)
  routeData: any
  protected consortiumMemberId = signal<string | undefined>(undefined)
  protected currentMonth = signal<number | undefined>(undefined)
  protected currentYear = signal<number | undefined>(undefined)
  protected monthList = signal<[string, string][] | undefined>(undefined)
  protected yearList = signal<number[] | undefined>(undefined)
  editForm: FormGroup = this.fb.group({
    terminationMonth: [null, [Validators.required]],
    terminationYear: [null, [Validators.required]],
  })

  ngOnInit() {
    this.activatedRoute.params.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      if (params['id']) {
        this.consortiumMemberId.set(params['id'])
      }
    })

    this.currentMonth.set(this.dateUtilService.getCurrentMonthNumber())
    this.currentYear.set(this.dateUtilService.getCurrentYear())
    this.monthList.set(this.dateUtilService.getMonthsList())

    this.yearList.set(this.dateUtilService.getFutureYearsIncludingCurrent(1))

    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        this.memberService.getMemberData(account.memberId).subscribe((data) => {
          if (data) {
            this.memberData.set(data)
            if (data.consortiumMembers) {
              this.consortiumMember.set(
                Object.values(data.consortiumMembers!).find(
                  (member: ISFConsortiumMemberData) => member.salesforceId === this.consortiumMemberId()
                )
              )
            }
          }
        })
      }
    })

    this.editForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.invalidForm.set(false)
      }
    })
  }

  createConsortiumMemberFromForm(): ISFConsortiumMemberData {
    return {
      ...new SFConsortiumMemberData(),
      terminationMonth: this.editForm.get('terminationMonth')?.value,
      terminationYear: this.editForm.get('terminationYear')?.value,
      orgName: this.consortiumMember()?.orgName,
    }
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.editForm.markAllAsTouched()
      Object.keys(this.editForm.controls).forEach((key) => {
        this.editForm.get(key)?.markAsDirty()
      })
      this.invalidForm.set(true)
    } else {
      this.invalidForm.set(false)
      this.isSaving.set(true)
      const consortiumMember = this.createConsortiumMemberFromForm()

      this.memberService.removeConsortiumMember(consortiumMember).subscribe({
        next: (res) => {
          if (res) {
            this.onSaveSuccess(consortiumMember.orgName)
          } else {
            console.error(res)
            this.onSaveError()
          }
        },
        error: (err) => {
          console.error(err)
          this.onSaveError()
        },
      })
    }
  }

  onSaveSuccess(orgName: string | undefined) {
    this.isSaving.set(false)
    this.alertService.broadcast(AlertType.CONSORTIUM_MEMBER_REMOVED, orgName)
    this.router.navigate([''])
  }

  onSaveError() {
    this.isSaving.set(false)
  }
}
