import { KeyValue } from '@angular/common'
import { Component, OnDestroy, OnInit } from '@angular/core'
import { FormBuilder, FormControl, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { EMPTY, Subject, combineLatest } from 'rxjs'
import { switchMap, take, takeUntil } from 'rxjs/operators'
import { AccountService } from 'src/app/account'
import { IAccount } from 'src/app/account/model/account.model'
import { EMAIL_REGEXP, URL_REGEXP, ORCID_BASE_URL } from 'src/app/app.constants'
import { ISFAddress, SFAddress } from 'src/app/member/model/salesforce-address.model'
import { ISFCountry } from 'src/app/member/model/salesforce-country.model'
import { ISFState } from 'src/app/member/model/salesforce-country.model copy'
import { SFMemberContact } from 'src/app/member/model/salesforce-member-contact.model'
import { ISFMemberData, SFConsortiumMemberData, SFMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { ISFMemberUpdate, SFMemberUpdate } from 'src/app/member/model/salesforce-member-update.model'
import { MemberService } from 'src/app/member/service/member.service'

@Component({
  selector: 'app-member-info-edit',
  templateUrl: './member-info-edit.component.html',
  styleUrls: ['./member-info-edit.component.scss'],
})
export class MemberInfoEditComponent implements OnInit, OnDestroy {
  countries: ISFCountry[] | undefined
  country: ISFCountry | undefined
  states: ISFState[] | undefined
  account: IAccount | undefined | null
  memberData: ISFMemberData | undefined | null
  objectKeys = Object.keys
  objectValues = Object.values
  orgIdsTransformed: { id: string; name: string }[] = []
  ORCID_BASE_URL = ORCID_BASE_URL

  isSaving = false
  invalidForm: boolean | undefined
  managedMember: string | undefined
  destroy$ = new Subject()
  quillConfig = {
    toolbar: [['bold', 'italic'], [{ list: 'ordered' }, { list: 'bullet' }], ['link']],
  }
  quillStyles = {
    fontFamily: 'inherit',
    fontSize: '14px',
    letterSpacing: '0.25px',
    marginRight: '0',
  }

  editForm = this.fb.group({
    orgName: new FormControl<null | string>(null, [Validators.required, Validators.maxLength(41)]),
    street: new FormControl<null | string>(null, [Validators.maxLength(255)]),
    city: new FormControl<null | string>(null, [Validators.maxLength(40)]),
    state: new FormControl<null | string>(null, [Validators.maxLength(80)]),
    country: new FormControl<null | string>(null, [Validators.required]),
    postcode: new FormControl<null | string>(null, [Validators.maxLength(20)]),
    trademarkLicense: new FormControl<null | string>(null, [Validators.required]),
    publicName: new FormControl<null | string>(null, [Validators.required, Validators.maxLength(255)]),
    description: new FormControl<null | string>(null, [Validators.maxLength(5000)]),
    website: new FormControl<null | string>(null, [Validators.pattern(URL_REGEXP), Validators.maxLength(255)]),
    email: new FormControl<null | string>(null, [Validators.pattern(EMAIL_REGEXP), Validators.maxLength(80)]),
  })

  constructor(
    private memberService: MemberService,
    private accountService: AccountService,
    private fb: FormBuilder,
    protected activatedRoute: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    combineLatest([this.activatedRoute.params, this.accountService.getAccountData()])
      .pipe(
        switchMap(([params, account]) => {
          if (params['id']) {
            this.managedMember = params['id']
          }
          if (account) {
            this.account = account
            if (this.managedMember) {
              this.memberService.setManagedMember(params['id'])
              return this.memberService.getMemberData(this.managedMember)
            } else {
              return this.memberService.getMemberData(account?.salesforceId)
            }
          } else {
            return EMPTY
          }
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((data) => {
        this.memberData = data
        this.memberData = new SFMemberData(
          '123',
          true,
          '123',
          true,
          'rara',
          'roro',
          'hehe.com',
          'Lithuania',
          'ruru',
          'riri',
          'rere',
          'hehe@mail.com',
          '2022',
          '2050',
          '',
          [new SFConsortiumMemberData('he', 'ho'), new SFConsortiumMemberData('hee', 'hoo')],
          [
            new SFMemberContact('124', true, ['goob'], 'name', 'phone', 'email'),
            new SFMemberContact('124', false, ['goob'], 'name', 'phone', 'email'),
          ],
          { ROR: ['123', '456'], GRID: ['1213', '1415'] },
          new SFAddress('street', 'United Kingdom', 'state', 'Lithuania', 'code', 'postalCode', 'city')
        )

        this.orgIdsTransformed = Object.entries(this.memberData?.orgIds || {}).flatMap(([name, ids]) =>
          ids.map((id: string) => ({ id, name }))
        )

        this.validateUrl()
        if (data) {
          this.updateForm(data)
        }
      })
    this.memberService
      .getCountries()
      .pipe(take(1))
      .subscribe((countries) => {
        this.countries = countries
        if (this.memberData) {
          this.updateForm(this.memberData)
        }
      })

    this.editForm.valueChanges.subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.invalidForm = false
      }
    })
  }

  validateUrl() {
    if (this.memberData?.website && !/(http(s?)):\/\//i.test(this.memberData.website)) {
      this.memberData.website = 'http://' + this.memberData.website
    }
  }

  updateForm(data: ISFMemberData) {
    if (data && data.id) {
      this.editForm.patchValue({
        orgName: data.name || null,
        trademarkLicense: data.trademarkLicense ? data.trademarkLicense : 'No',
        publicName: data.publicDisplayName,
        description: data.publicDisplayDescriptionHtml,
        website: data.website,
        email: data.publicDisplayEmail,
      })
      if (data.billingAddress) {
        if (this.countries) {
          this.country = this.countries.find((country) => country.name === data.billingAddress?.country)
          if (this.country) {
            this.states = this.country.states
          } else {
            console.error('Unable to find country: ', data.billingAddress.country)
          }
        }
        this.editForm.patchValue({
          street: data.billingAddress.street,
          city: data.billingAddress.city,
          state: data.billingAddress.state,
          country: data.billingAddress.country,
          postcode: data.billingAddress.postalCode,
        })
      }
    }
  }

  filterCRFID(id: string) {
    return id.replace(/^.*dx.doi.org\//g, '')
  }

  createDetailsFromForm(): ISFMemberUpdate {
    const address: ISFAddress = {
      street: this.editForm.get(['street'])?.value,
      city: this.editForm.get(['city'])?.value,
      state:
        this.editForm.get(['state'])?.value == '-- No state or province --'
          ? undefined
          : this.editForm.get(['state'])?.value,
      country: this.editForm.get(['country'])?.value,
      countryCode: this.country?.code,
      postalCode: this.editForm.get(['postcode'])?.value,
    }
    return {
      ...new SFMemberUpdate(),
      orgName: this.editForm.get(['orgName'])?.value,
      billingAddress: address,
      trademarkLicense: this.editForm.get(['trademarkLicense'])?.value,
      publicName: this.editForm.get(['publicName'])?.value,
      description: this.editForm.get(['description'])?.value,
      website: this.editForm.get(['website'])?.value,
      email: this.editForm.get(['email'])?.value,
    }
  }

  save() {
    if (this.editForm.status === 'INVALID') {
      this.invalidForm = true
      this.editForm.markAllAsTouched()
      Object.keys(this.editForm.controls).forEach((key) => {
        this.editForm.get(key)?.markAsDirty()
      })
    } else {
      this.invalidForm = false
      this.isSaving = true
      const details: ISFMemberUpdate = this.createDetailsFromForm()

      if (this.memberData?.id) {
        this.memberService.updateMemberDetails(details, this.memberData?.id).subscribe({
          next: () => {
            this.memberService.setMemberData({
              ...this.memberData,
              publicDisplayDescriptionHtml: details.description,
              publicDisplayName: details.publicName,
              name: details.orgName,
              billingAddress: details.billingAddress,
              trademarkLicense: details.trademarkLicense,
              publicDisplayEmail: details.email,
              website: details.website,
            })
            this.onSaveSuccess()
          },
          error: (err: any) => {
            console.error(err)
            this.onSaveError()
          },
        })
      }
    }
  }

  ngOnDestroy() {
    this.destroy$.next(true)
    this.destroy$.complete()
  }

  onSaveSuccess() {
    this.isSaving = false
    this.router.navigate([''])
  }

  onSaveError() {
    this.isSaving = false
  }
}
