import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { AbstractControl, FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { combineLatest, take } from 'rxjs'
import { AccountService } from 'src/app/account'
import { AlertType, EMAIL_REGEXP } from '../../app.constants'
import { ISFCountry } from '../../member/model/salesforce-country.model'
import { ISFState } from '../../member/model/salesforce-country.model copy'
import { ISFMemberData } from '../../member/model/salesforce-member-data.model'
import {
  ISFNewConsortiumMember,
  OrganizationTierOption,
  TrademarkLicenseOption,
} from '../../member/model/salesforce-new-consortium-member.model'
import { MemberService } from '../../member/service/member.service'
import { AlertService } from '../../shared/service/alert.service'
import { DateUtilService } from '../../shared/service/date-util.service'

@Component({
  selector: 'app-add-consortium-member',
  templateUrl: './add-consortium-member.component.html',
  styleUrls: ['./add-consortium-member.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ReactiveFormsModule],
})
export class AddConsortiumMemberComponent implements OnInit {
  private memberService = inject(MemberService)
  private fb = inject(FormBuilder)
  private alertService = inject(AlertService)
  private router = inject(Router)
  private dateUtilService = inject(DateUtilService)
  private accountService = inject(AccountService)
  protected activatedRoute = inject(ActivatedRoute)
  private destroyRef = inject(DestroyRef)

  protected countries = signal<ISFCountry[] | undefined>(undefined)
  protected states = signal<ISFState[] | undefined>(undefined)
  protected memberData = signal<ISFMemberData | undefined | null>(null)
  protected isSaving = signal(false)
  protected invalidForm = signal(false)
  routeData: any
  protected currentMonth = signal<number | undefined>(undefined)
  protected currentYear = signal<number | undefined>(undefined)
  protected monthList = signal<[string, string][] | undefined>(undefined)
  protected yearList = signal<number[] | undefined>(undefined)
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

  protected rolesData = [
    { id: 1, selected: false, name: 'Agreement signatory (OFFICIAL)' },
    { id: 2, selected: false, name: 'Main relationship contact (OFFICIAL)' },
    { id: 3, selected: false, name: 'Voting contact' },
    { id: 4, selected: false, name: 'Technical contact' },
    { id: 5, selected: false, name: 'Invoice contact' },
    { id: 6, selected: false, name: 'Comms contact' },
    { id: 7, selected: false, name: 'Product contact' },
    { id: 8, selected: false, name: 'Other contact' },
  ]

  protected trademarkLicenseOptions: TrademarkLicenseOption[] = [
    {
      value: 'Yes',
      description: `ORCID can use this organization's trademarked name and logos`,
    },
    {
      value: 'No',
      description: `ORCID cannot use this organization's trademarked name and logos`,
    },
  ]

  protected organizationTiers: OrganizationTierOption[] = [
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
  ]

  ngOnInit() {
    this.currentMonth.set(this.dateUtilService.getCurrentMonthNumber())
    this.currentYear.set(this.dateUtilService.getCurrentYear())
    this.monthList.set(this.dateUtilService.getMonthsList())
    this.yearList.set(this.dateUtilService.getFutureYearsIncludingCurrent(1))

    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        combineLatest([this.memberService.getMemberData(account.memberId), this.memberService.getCountries()])
          .pipe(take(1))
          .subscribe(([data, countries]) => {
            this.memberData.set(data)
            this.countries.set(countries)
          })
      }
    })

    this.editForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.editForm!.status === 'VALID') {
        this.invalidForm.set(false)
      }
    })
  }

  getControl(name: string): AbstractControl {
    return this.editForm.get(name)!
  }

  getFormValue(controlName: string): string {
    return this.getControl(controlName)?.value
  }

  trackByValue(_idx: number, tier: OrganizationTierOption | TrademarkLicenseOption): string {
    return tier.value
  }

  createNewConsortiumMemberFromForm(): ISFNewConsortiumMember {
    const stateValue = this.getFormValue('state')
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

  onCountryChange(countryName: string | null | undefined) {
    const availableCountries = this.countries()
    if (!countryName || !availableCountries) {
      this.states.set(undefined)
      return
    }

    this.states.set(availableCountries.find((country) => country.name === countryName)?.states)
  }

  save() {
    if (this.editForm!.status === 'INVALID') {
      Object.keys(this.editForm!.controls).forEach((key) => {
        this.editForm!.get(key)?.markAsDirty()
      })
      this.editForm!.markAllAsTouched()
      this.invalidForm.set(true)
    } else {
      this.invalidForm.set(false)
      this.isSaving.set(true)
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
    this.isSaving.set(false)
    this.alertService.broadcast(AlertType.CONSORTIUM_MEMBER_ADDED, orgName)
    this.router.navigate([''])
  }

  onSaveError() {
    this.isSaving.set(false)
  }
}
