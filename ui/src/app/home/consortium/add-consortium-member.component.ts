import { Component, OnInit } from '@angular/core'
import { FormGroup, FormBuilder, Validators } from '@angular/forms'
import { Router, ActivatedRoute } from '@angular/router'
import { combineLatest, take } from 'rxjs'
import { AccountService } from '../../account'
import { AlertType, EMAIL_REGEXP } from '../../app.constants'
import { AlertService } from '../../shared/service/alert.service'
import { DateUtilService } from '../../shared/service/date-util.service'
import { ISFCountry } from '../../member/model/salesforce-country.model'
import { ISFState } from '../../member/model/salesforce-country.model copy'
import { ISFMemberData } from '../../member/model/salesforce-member-data.model'
import { ISFNewConsortiumMember } from '../../member/model/salesforce-new-consortium-member.model'
import { MemberService } from '../../member/service/member.service'

@Component({
  selector: 'app-add-consortium-member',
  templateUrl: './add-consortium-member.component.html',
  styleUrls: ['./add-consortium-member.component.scss'],
})
export class AddConsortiumMemberComponent implements OnInit {
  countries: ISFCountry[] | undefined
  states: ISFState[] | undefined
  memberData: ISFMemberData | undefined | null
  isSaving = false
  invalidForm = false
  routeData: any
  currentMonth: number | undefined
  currentYear: number | undefined
  monthList: [string, string][] | undefined
  yearList: number[] | undefined
  editForm: FormGroup = this.fb.group({
    orgName: [null, [Validators.required, Validators.maxLength(41)]],
    emailDomain: [null, [Validators.maxLength(255)]],
    street: [null, [Validators.maxLength(255)]],
    city: [null, [Validators.maxLength(40)]],
    state: [null, [Validators.maxLength(80)]],
    country: [null, [Validators.required]],
    postcode: [null, [Validators.maxLength(20)]],
    trademarkLicense: [null, [Validators.required]],
    startMonth: [null, [Validators.required]],
    startYear: [null, [Validators.required]],
    contactGivenName: [null, [Validators.required, Validators.maxLength(40)]],
    contactFamilyName: [null, [Validators.required, Validators.maxLength(80)]],
    contactJobTitle: [null, [Validators.maxLength(128)]],
    contactEmail: [null, [Validators.required, Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]],
  })

  rolesData = [
    { id: 1, selected: false, name: 'Agreement signatory (OFFICIAL)' },
    { id: 2, selected: false, name: 'Main relationship contact (OFFICIAL)' },
    { id: 3, selected: false, name: 'Voting contact' },
    { id: 4, selected: false, name: 'Technical contact' },
    { id: 5, selected: false, name: 'Invoice contact' },
    { id: 6, selected: false, name: 'Comms contact' },
    { id: 7, selected: false, name: 'Product contact' },
    { id: 8, selected: false, name: 'Other contact' },
  ]

  constructor(
    private memberService: MemberService,
    private fb: FormBuilder,
    private alertService: AlertService,
    private router: Router,
    private dateUtilService: DateUtilService,
    private accountService: AccountService,
    protected activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.currentMonth = this.dateUtilService.getCurrentMonthNumber()
    this.currentYear = this.dateUtilService.getCurrentYear()
    this.monthList = this.dateUtilService.getMonthsList()
    this.yearList = this.dateUtilService.getFutureYearsIncludingCurrent(1)

    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        combineLatest([this.memberService.getMemberData(account.salesforceId), this.memberService.getCountries()])
          .pipe(take(1))
          .subscribe(([data, countries]) => {
            this.memberData = data
            this.countries = countries
          })
      }
    })

    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm!.status === 'VALID') {
        this.invalidForm = false
      }
    })
  }

  createNewConsortiumMemberFromForm(): ISFNewConsortiumMember {
    const consortiumMember: ISFNewConsortiumMember = {
      orgName: this.editForm!.get('orgName')?.value,
      trademarkLicense: this.editForm!.get('trademarkLicense')?.value,
      startMonth: this.editForm!.get('startMonth')?.value,
      startYear: this.editForm!.get('startYear')?.value,
      emailDomain: this.editForm!.get('emailDomain')?.value,
      street: this.editForm!.get('street')?.value,
      city: this.editForm!.get('city')?.value,
      state:
        this.editForm!.get(['state'])?.value == '-- No state or province --'
          ? null
          : this.editForm!.get(['state'])?.value,
      country: this.editForm!.get('country')?.value,
      postcode: this.editForm!.get('postcode')?.value,
      contactGivenName: this.editForm!.get('contactGivenName')?.value,
      contactFamilyName: this.editForm!.get('contactFamilyName')?.value,
      contactJobTitle: this.editForm!.get('contactJobTitle')?.value,
      contactEmail: this.editForm!.get('contactEmail')?.value,
    }
    return consortiumMember
  }

  onCountryChange(countryName: string) {
    this.states = this.countries!.find((country) => country.name === countryName)?.states
  }

  save() {
    if (this.editForm!.status === 'INVALID') {
      Object.keys(this.editForm!.controls).forEach((key) => {
        this.editForm!.get(key)?.markAsDirty()
      })
      this.editForm!.markAllAsTouched()
      this.invalidForm = true
    } else {
      this.invalidForm = false
      this.isSaving = true
      const newConsortiumMember = this.createNewConsortiumMemberFromForm()

      this.memberService.addConsortiumMember(newConsortiumMember).subscribe(
        (res) => {
          if (res) {
            this.onSaveSuccess(newConsortiumMember.orgName)
          } else {
            console.error(res)
            this.onSaveError()
          }
        },
        (err) => {
          console.error(err)
          this.onSaveError()
        }
      )
    }
  }

  onSaveSuccess(orgName: string) {
    this.isSaving = false
    this.alertService.broadcast(AlertType.CONSORTIUM_MEMBER_ADDED, orgName)
    this.router.navigate([''])
  }

  onSaveError() {
    this.isSaving = false
  }
}
