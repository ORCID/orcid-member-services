import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormBuilder, FormControl, Validators, ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { EMPTY, combineLatest } from 'rxjs'
import { switchMap, take } from 'rxjs/operators'
import { AccountService } from 'src/app/account'
import { IAccount } from 'src/app/account/model/account.model'
import { EMAIL_REGEXP, URL_REGEXP, ORCID_BASE_URL } from 'src/app/app.constants'
import { ISFAddress } from 'src/app/member/model/salesforce-address.model'
import { ISFCountry } from 'src/app/member/model/salesforce-country.model'
import { ISFState } from 'src/app/member/model/salesforce-country.model copy'
import { ISFMemberData } from 'src/app/member/model/salesforce-member-data.model'
import { ISFMemberUpdate, SFMemberUpdate } from 'src/app/member/model/salesforce-member-update.model'
import { MemberService } from 'src/app/member/service/member.service'
import { QuillEditorComponent } from 'ngx-quill'

@Component({
  selector: 'app-member-info-edit',
  templateUrl: './member-info-edit.component.html',
  styleUrls: ['./member-info-edit.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ReactiveFormsModule, QuillEditorComponent],
})
export class MemberInfoEditComponent implements OnInit {
  private memberService = inject(MemberService)
  private accountService = inject(AccountService)
  private fb = inject(FormBuilder)
  protected activatedRoute = inject(ActivatedRoute)
  private router = inject(Router)
  private destroyRef = inject(DestroyRef)

  protected countries = signal<ISFCountry[] | undefined>(undefined)
  protected country = signal<ISFCountry | undefined>(undefined)
  protected states = signal<ISFState[] | undefined>(undefined)
  protected account = signal<IAccount | undefined | null>(null)
  protected memberData = signal<ISFMemberData | undefined | null>(null)
  protected objectKeys = Object.keys
  protected orgIdsTransformed = signal<{ id: string; name: string }[]>([])
  protected ORCID_BASE_URL = ORCID_BASE_URL

  protected isSaving = signal(false)
  protected invalidForm = signal(false)
  protected managedMember = signal<string | undefined>(undefined)
  protected quillConfig = {
    toolbar: [['bold', 'italic'], [{ list: 'ordered' }, { list: 'bullet' }], ['link']],
  }
  protected quillStyles = {
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

  ngOnInit() {
    combineLatest([this.activatedRoute.params, this.accountService.getAccountData(), this.memberService.getCountries()])
      .pipe(
        switchMap(([params, account, countries]) => {
          this.countries.set(countries)
          if (params['id']) {
            this.managedMember.set(params['id'])
          }
          if (account) {
            this.account.set(account)
            if (this.managedMember()) {
              this.memberService.setManagedMember(params['id'])
              return this.memberService.getMemberData(this.managedMember())
            } else {
              return this.memberService.getMemberData(account?.memberId)
            }
          } else {
            return EMPTY
          }
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((data) => {
        this.memberData.set(data)
        this.orgIdsTransformed.set(
          Object.entries(this.memberData()?.orgIds || {}).flatMap(([name, ids]) =>
            ids.map((id: string) => ({ id, name }))
          )
        )
        this.validateUrl()
        if (data) {
          this.updateForm(data)
        }
      })

    this.editForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.editForm.status === 'VALID') {
        this.invalidForm.set(false)
      }
    })
  }

  validateUrl() {
    const memberData = this.memberData()
    if (memberData?.website && !/(http(s?)):\/\//i.test(memberData.website)) {
      this.memberData.set({ ...memberData, website: 'http://' + memberData.website })
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
        if (this.countries()) {
          this.country.set(this.countries()!.find((country) => country.name === data.billingAddress?.country))
          if (this.country()) {
            this.states.set(this.country()!.states)
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
      countryCode: this.country()?.code,
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
      this.invalidForm.set(true)
      this.editForm.markAllAsTouched()
      Object.keys(this.editForm.controls).forEach((key) => {
        this.editForm.get(key)?.markAsDirty()
      })
    } else {
      this.invalidForm.set(false)
      this.isSaving.set(true)
      const details: ISFMemberUpdate = this.createDetailsFromForm()

      const memberId = this.memberData()?.memberId
      if (memberId) {
        this.memberService.updateMemberDetails(details, memberId).subscribe({
          next: () => {
            this.memberService.setMemberData({
              ...this.memberData(),
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

  onSaveSuccess() {
    this.isSaving.set(false)
    this.router.navigate([''])
  }

  onSaveError() {
    this.isSaving.set(false)
  }
}
