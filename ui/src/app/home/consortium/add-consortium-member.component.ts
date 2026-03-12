import { Component, OnInit } from '@angular/core'
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { combineLatest, take } from 'rxjs'
import { AccountService } from 'src/app/account'
import { AlertType, EMAIL_REGEXP } from '../../app.constants'
import { ISFCountry } from '../../member/model/salesforce-country.model'
import { ISFState } from '../../member/model/salesforce-country.model copy'
import { ISFMemberData } from '../../member/model/salesforce-member-data.model'
import { ISFNewConsortiumMember, OrganizationTierOption, TrademarkLicenseOption } from '../../member/model/salesforce-new-consortium-member.model'
import { MemberService } from '../../member/service/member.service'
import { AlertService } from '../../shared/service/alert.service'
import { DateUtilService } from '../../shared/service/date-util.service'

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
    country: [null],
    postcode: [null, [Validators.maxLength(20)]],
    trademarkLicense: [null, [Validators.required]],
    startMonth: [null, [Validators.required]],
    startYear: [null, [Validators.required]],
    contactGivenName: [null, [Validators.maxLength(40)]],
    contactFamilyName: [null, [Validators.maxLength(80)]],
    contactJobTitle: [null, [Validators.maxLength(128)]],
    contactEmail: [null, [Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]],
    organizationTier: [null, [Validators.required]],
    integrationPlans: [null, [Validators.maxLength(1000)]],
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


  trademarkLicenseOptions: TrademarkLicenseOption[] = [
    {
      value: 'Yes',
      description: `ORCID can use this organization's trademarked name and logos`,
    },
    {
      value: 'No',
      description: `ORCID cannot use this organization's trademarked name and logos`,
    },
  ];

  organizationTiers: OrganizationTierOption[] = [
    {
      value: 'Small',
      description: `Legal entity's annual operating budget below 10 M USD`,
    },
    {
      value: 'Standard',
      description: `Legal entity's annual operating budget between 10 M and 1 B USD`,
    },
    {
      value: 'Large',
      description: `Legal entity's annual operating budget above 1 B USD`,
    },
  ];

  constructor(
    private memberService: MemberService,
    private fb: FormBuilder,
    private alertService: AlertService,
    private router: Router,
    private dateUtilService: DateUtilService,
    private accountService: AccountService,
    protected activatedRoute: ActivatedRoute
  ) { }

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

  getControl(name: string): AbstractControl {
    return this.editForm.get(name)!;
  }

  getFormValue(controlName: string): string {
    return this.getControl(controlName)?.value;
  }

  trackByValue(_idx: number, tier: OrganizationTierOption | TrademarkLicenseOption): string {
    return tier.value;
  }

  createNewConsortiumMemberFromForm(): ISFNewConsortiumMember {
    const stateValue = this.getFormValue('state');
    return {
      orgName: this.getFormValue('orgName'),
      trademarkLicense: this.getFormValue('trademarkLicense'),
      startMonth: this.getFormValue('startMonth'),
      startYear: this.getFormValue('startYear'),
      emailDomain: this.getFormValue('emailDomain'),
      street: this.getFormValue('street'),
      city: this.getFormValue('city'),
      state: stateValue === '-- No state or province --' ? undefined : stateValue,
      country: this.getFormValue('country'),
      postcode: this.getFormValue('postcode'),
      contactGivenName: this.getFormValue('contactGivenName'),
      contactFamilyName: this.getFormValue('contactFamilyName'),
      contactJobTitle: this.getFormValue('contactJobTitle'),
      contactEmail: this.getFormValue('contactEmail'),
      organizationTier: this.getFormValue('organizationTier'),
      integrationPlans: this.getFormValue('integrationPlans'),
    }
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
