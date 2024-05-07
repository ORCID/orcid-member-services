import { Component, OnDestroy, OnInit } from '@angular/core'
import { FormGroup, FormBuilder, Validators } from '@angular/forms'
import { Router, ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs'
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
})
export class RemoveConsortiumMemberComponent implements OnInit, OnDestroy {
  memberDataSubscription: Subscription | undefined
  memberData: ISFMemberData | undefined | null
  consortiumMember: SFConsortiumMemberData | undefined
  isSaving = false
  invalidForm = false
  routeData: any
  consortiumMemberId: string | undefined
  currentMonth: number | undefined
  currentYear: number | undefined
  monthList: [string, string][] | undefined
  yearList: number[] | undefined
  editForm: FormGroup = this.fb.group({
    terminationMonth: [null, [Validators.required]],
    terminationYear: [null, [Validators.required]],
  })

  constructor(
    private memberService: MemberService,
    private accountService: AccountService,
    private fb: FormBuilder,
    private alertService: AlertService,
    private router: Router,
    private dateUtilService: DateUtilService,
    protected activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      if (params['id']) {
        this.consortiumMemberId = params['id']
      }
    })

    this.currentMonth = this.dateUtilService.getCurrentMonthNumber()
    this.currentYear = this.dateUtilService.getCurrentYear()
    this.monthList = this.dateUtilService.getMonthsList()

    this.yearList = this.dateUtilService.getFutureYearsIncludingCurrent(1)

    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        this.memberService.getMemberData(account.salesforceId).subscribe((data) => {
          if (data) {
            this.memberData = data
            if (data.consortiumMembers) {
              this.consortiumMember = Object.values(data.consortiumMembers!).find(
                (member: ISFConsortiumMemberData) => member.salesforceId === this.consortiumMemberId
              )
            }
          }
        })
      }
    })

    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.invalidForm = false
      }
    })
  }

  ngOnDestroy(): void {
    if (this.memberDataSubscription) {
      this.memberDataSubscription.unsubscribe()
    }
  }

  createConsortiumMemberFromForm(): ISFConsortiumMemberData {
    return {
      ...new SFConsortiumMemberData(),
      terminationMonth: this.editForm.get('terminationMonth')?.value,
      terminationYear: this.editForm.get('terminationYear')?.value,
      orgName: this.consortiumMember?.orgName,
    }
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.editForm.markAllAsTouched()
      Object.keys(this.editForm.controls).forEach((key) => {
        this.editForm.get(key)?.markAsDirty()
      })
      this.invalidForm = true
    } else {
      this.invalidForm = false
      this.isSaving = true
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
    this.isSaving = false
    this.alertService.broadcast(AlertType.CONSORTIUM_MEMBER_REMOVED, orgName)
    this.router.navigate([''])
  }

  onSaveError() {
    this.isSaving = false
  }
}
