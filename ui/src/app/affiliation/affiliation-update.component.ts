import { Component, OnInit } from '@angular/core'
import { AbstractControl, FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms'
import { ActivatedRoute } from '@angular/router'
import * as moment from 'moment'
import { IAffiliation, Affiliation } from './model/affiliation.model'
import { AffiliationService } from './service/affiliations.service'
import { DateUtilService } from '../shared/service/date-util.service'

import {
  DATE_TIME_FORMAT,
  EMAIL_REGEXP,
  AFFILIATION_TYPES,
  COUNTRIES,
  ORG_ID_TYPES,
  DEFAULT_LATEST_YEAR_INCREMENT,
} from '../app.constants'
import { AlertService } from '../shared/service/alert.service'
import { faBan, faSave } from '@fortawesome/free-solid-svg-icons'

function dateValidator() {
  return (formGroup: FormGroup) => {
    const startYearControl = formGroup.controls['startYear']
    const endYearControl = formGroup.controls['endYear']
    const startMonthControl = formGroup.controls['startMonth']
    const endMonthControl = formGroup.controls['endMonth']
    const startDayControl = formGroup.controls['startDay']
    const endDayControl = formGroup.controls['endDay']

    if (
      (hasValue(startDayControl) && hasValue(endDayControl)) ||
      (hasValue(startMonthControl) && hasValue(endMonthControl)) ||
      (hasValue(startYearControl) && hasValue(endYearControl))
    ) {
      const startDate = new Date(
        hasValue(startYearControl) ? startYearControl.value : 0,
        hasValue(startMonthControl) ? startMonthControl.value : 0,
        hasValue(startDayControl) ? startDayControl.value : 0
      )
      const endDate = new Date(
        hasValue(endYearControl) ? endYearControl.value : 0,
        hasValue(endMonthControl) ? endMonthControl.value : 0,
        hasValue(endDayControl) ? endDayControl.value : 0
      )

      if (startDate > endDate) {
        endYearControl.setErrors({ dateValidator: true })
      } else {
        endYearControl.setErrors(null)
      }
    }
  }
}

function disambiguatedOrgIdValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: boolean } | null => {
    if (control.parent !== undefined) {
      const disambiguationSourceValue = control.parent?.get('disambiguationSource')?.value
      if (disambiguationSourceValue === 'RINGGOLD') {
        const reg = new RegExp('^\\d+$')
        if (control.value && !reg.test(control.value)) {
          return { validDisambiguatedOrgId: false }
        }
      } else if (disambiguationSourceValue === 'GRID') {
        const gridStartsWith = 'grid.'
        const gridBaseUrl = 'https://www.grid.ac/'
        const gridBaseUrlInstitutes = 'https://www.grid.ac/institutes/'
        const gridBaseUrlAlt = 'https://grid.ac/'
        const gridBaseUrlInstitutesAlt = 'https://grid.ac/institutes/'
        let gridId = control.value
        // strip the url and see if is a valid grid id
        if (gridId && gridId.substr(0, gridBaseUrlInstitutes.length) === gridBaseUrlInstitutes) {
          gridId = gridId.substr(gridBaseUrlInstitutes.length, gridId.length)
        } else if (gridId && gridId.substr(0, gridBaseUrl.length) === gridBaseUrl) {
          gridId = gridId.substr(gridBaseUrl.length)
        } else if (gridId && gridId.substr(0, gridBaseUrlInstitutesAlt.length) === gridBaseUrlInstitutesAlt) {
          gridId = gridId.substr(gridBaseUrlInstitutesAlt.length)
        } else if (gridId && gridId.substr(0, gridBaseUrlAlt.length) === gridBaseUrlAlt) {
          gridId = gridId.substr(gridBaseUrlAlt.length)
        }

        if (gridId && !(gridId.substr(0, gridStartsWith.length) === gridStartsWith)) {
          return { validDisambiguatedOrgId: false }
        }
      } else if (disambiguationSourceValue === 'ROR') {
        const reg = new RegExp('^(https://ror.org/)?0[^ILO]{6}\\d{2}$')
        if (control.value && !reg.test(control.value)) {
          return { validDisambiguatedOrgId: false }
        }
      }
    }
    return null
  }
}

function hasValue(controls: AbstractControl): boolean {
  return controls && controls.value !== undefined && controls.value !== '' && controls.value !== null
}

function isValidDate(y: string, m: string, d: string) {
  let year = parseInt(y)
  let month = parseInt(m)
  let day = parseInt(d)
  const date = new Date(year, month - 1, day)

  return date.getUTCFullYear() === year && date.getUTCMonth() === month && date.getUTCDate() === day
}

@Component({
  selector: 'app-affiliation-update',
  templateUrl: './affiliation-update.component.html',
})
export class AffiliationUpdateComponent implements OnInit {
  AFFILIATION_TYPES = AFFILIATION_TYPES
  COUNTRIES = COUNTRIES
  ORG_ID_TYPES = ORG_ID_TYPES
  startYearsList: any
  endYearsList: any
  monthsList: any
  startDaysList: any
  endDaysList: any
  isSaving = false
  ngbDate: any
  faBan = faBan
  faSave = faSave

  editForm = this.fb.group(
    {
      id: [''],
      email: ['', [Validators.pattern(EMAIL_REGEXP), Validators.required]],
      affiliationSection: ['', [Validators.required]],
      departmentName: ['', [Validators.maxLength(4000)]],
      roleTitle: ['', [Validators.maxLength(4000)]],
      url: ['', [Validators.maxLength(8000)]],
      startYear: [null],
      startMonth: [null],
      startDay: [null],
      endYear: [null],
      endMonth: [null],
      endDay: [null],
      orgName: ['', [Validators.required]],
      orgCountry: ['', [Validators.required]],
      orgCity: ['', [Validators.required]],
      orgRegion: [''],
      disambiguatedOrgId: ['', [Validators.required, disambiguatedOrgIdValidator()]],
      disambiguationSource: ['', [Validators.required]],
      externalId: [''],
      externalIdType: [''],
      externalIdUrl: [''],
      putCode: [''],
      created: [''],
      modified: [''],
      deletedFromORCID: [''],
      status: [''],
      ownerId: [''],
      sent: [''],
    },
    { validators: dateValidator() }
  )

  constructor(
    protected affiliationService: AffiliationService,
    protected dateUtilService: DateUtilService,
    protected activatedRoute: ActivatedRoute,
    private alertService: AlertService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.startYearsList = this.dateUtilService.getYearsList(0)
    this.endYearsList = this.dateUtilService.getYearsList(DEFAULT_LATEST_YEAR_INCREMENT)
    this.monthsList = this.dateUtilService.getMonthsList()
    this.startDaysList = this.dateUtilService.getDaysList()
    this.endDaysList = this.dateUtilService.getDaysList()
    this.isSaving = false
    this.activatedRoute.data.subscribe(({ assertion }) => {
      this.updateForm(assertion)
    })

    this.onChanges()
  }

  onChanges(): void {
    this.editForm.get('startMonth')?.valueChanges.subscribe((val) => {
      this.startDaysList = this.dateUtilService.getDaysList(
        this.editForm.get('startYear')?.value || undefined,
        this.editForm.get('startMonth')?.value || undefined
      )
    })
    this.editForm.get('endMonth')?.valueChanges.subscribe((val) => {
      this.endDaysList = this.dateUtilService.getDaysList(
        this.editForm.get('endYear')?.value || undefined,
        this.editForm.get('endMonth')?.value || undefined
      )
    })
    this.editForm.get('disambiguationSource')?.valueChanges.subscribe((value) => {
      this.editForm.get('disambiguatedOrgId')?.markAsTouched()
      this.editForm.get('disambiguatedOrgId')?.updateValueAndValidity()
    })
  }

  updateForm(assertion: IAffiliation) {
    if (assertion?.id) {
      this.editForm.patchValue({
        id: assertion.id,
        email: assertion.email?.trim(),
        affiliationSection: assertion.affiliationSection,
        departmentName: assertion.departmentName,
        roleTitle: assertion.roleTitle,
        url: assertion.url,
        startYear: assertion.startYear,
        startMonth: parseInt(assertion.startMonth || '0'),
        startDay: assertion.startDay,
        endYear: assertion.endYear,
        endMonth: parseInt(assertion.endMonth || '0'),
        endDay: assertion.endDay,
        orgName: assertion.orgName,
        orgCountry: assertion.orgCountry,
        orgCity: assertion.orgCity,
        orgRegion: assertion.orgRegion,
        disambiguatedOrgId: assertion.disambiguatedOrgId,
        disambiguationSource: assertion.disambiguationSource,
        externalId: assertion.externalId,
        externalIdType: assertion.externalIdType,
        externalIdUrl: assertion.externalIdUrl,
        putCode: assertion.putCode,
        created: assertion.created != null ? assertion.created.format(DATE_TIME_FORMAT) : null,
        modified: assertion.modified != null ? assertion.modified.format(DATE_TIME_FORMAT) : null,
        deletedFromORCID:
          assertion.deletedFromORCID != null ? assertion.deletedFromORCID.format(DATE_TIME_FORMAT) : null,
        status: assertion.status,
        ownerId: assertion.ownerId,
      })

      this.onStartDateSelected(false)
      this.onEndDateSelected(false)
    }
  }

  previousState() {
    window.history.back()
  }

  save() {
    this.isSaving = true
    const assertion = this.createFromForm()

    if (assertion.id !== undefined && assertion.id != null) {
      this.affiliationService.update(assertion).subscribe({
        next: () => {
          this.onSaveSuccess()
          this.alertService.broadcast('assertionServiceApp.affiliation.updated.string')
        },
        error: (err) => this.onSaveError(err),
      })
    } else {
      this.affiliationService.create(assertion).subscribe({
        next: () => {
          this.onSaveSuccess()
          // TODO: add alerttype
          this.alertService.broadcast('assertionServiceApp.affiliation.created.string')
        },
        error: (err) => this.onSaveError(err),
      })
    }
  }

  private createFromForm(): IAffiliation {
    return {
      ...new Affiliation(),
      id: this.editForm.get(['id'])?.value,
      email: this.editForm.get(['email'])?.value,
      affiliationSection: this.editForm.get(['affiliationSection'])?.value,
      departmentName: this.editForm.get(['departmentName'])?.value,
      roleTitle: this.editForm.get(['roleTitle'])?.value,
      url: this.editForm.get(['url'])?.value,
      startYear: this.editForm.get(['startYear'])?.value,
      startMonth: this.editForm.get(['startMonth'])?.value,
      startDay: this.editForm.get(['startDay'])?.value,
      endYear: this.editForm.get(['endYear'])?.value,
      endMonth: this.editForm.get(['endMonth'])?.value,
      endDay: this.editForm.get(['endDay'])?.value,
      orgName: this.editForm.get(['orgName'])?.value,
      orgCountry: this.editForm.get(['orgCountry'])?.value,
      orgCity: this.editForm.get(['orgCity'])?.value,
      orgRegion: this.editForm.get(['orgRegion'])?.value,
      disambiguatedOrgId: this.editForm.get(['disambiguatedOrgId'])?.value,
      disambiguationSource: this.editForm.get(['disambiguationSource'])?.value,
      externalId: this.editForm.get(['externalId'])?.value,
      externalIdType: this.editForm.get(['externalIdType'])?.value,
      externalIdUrl: this.editForm.get(['externalIdUrl'])?.value,
      putCode: this.editForm.get(['putCode'])?.value,
      created:
        this.editForm.get(['created'])?.value != null
          ? moment(this.editForm.get(['created'])?.value, DATE_TIME_FORMAT)
          : undefined,
      modified:
        this.editForm.get(['modified'])?.value != null
          ? moment(this.editForm.get(['modified'])?.value, DATE_TIME_FORMAT)
          : undefined,
      deletedFromORCID:
        this.editForm.get(['deletedFromORCID'])?.value != null
          ? moment(this.editForm.get(['deletedFromORCID'])?.value, DATE_TIME_FORMAT)
          : undefined,
      status: this.editForm.get(['status']) ? this.editForm.get(['status'])?.value : '',
      ownerId: this.editForm.get(['ownerId']) ? this.editForm.get(['ownerId'])?.value : '',
    }
  }

  protected onSaveSuccess() {
    this.isSaving = false
    this.previousState()
  }

  protected onSaveError(err: any) {
    console.error(err)
    this.isSaving = false
  }

  public onStartDateSelected(resetValue: boolean) {
    this.startDaysList = this.dateUtilService.getDaysList(
      this.editForm.get('startYear')?.value || undefined,
      this.editForm.get('startMonth')?.value || undefined
    )

    if (resetValue && this.editForm.get('startDay')?.value) {
      if (this.editForm.get('startYear')?.value && this.editForm.get('startMonth')?.value) {
        if (
          !isValidDate(
            this.editForm.get('startYear')!.value!,
            this.editForm.get('startMonth')!.value!,
            this.editForm.get('startDay')!.value!
          )
        ) {
          this.editForm.patchValue({
            startDay: null,
          })
        }
      }
    }
  }

  public onEndDateSelected(resetValue: boolean) {
    if (resetValue && this.editForm.get('endDay')?.value) {
      if (this.editForm.get('endYear')?.value && this.editForm.get('endMonth')?.value) {
        this.endDaysList = this.dateUtilService.getDaysList(
          this.editForm.get('endYear')!.value!,
          this.editForm.get('endMonth')!.value!
        )
        if (
          !isValidDate(
            this.editForm.get('endYear')!.value!,
            this.editForm.get('endMonth')!.value!,
            this.editForm.get('endDay')!.value!
          )
        ) {
          this.editForm.patchValue({
            endDay: null,
          })
        }
      }
    }
  }
}
