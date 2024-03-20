import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationUpdateComponent } from './affiliation-update.component'
import { HttpClientModule } from '@angular/common/http'
import { RouterTestingModule } from '@angular/router/testing'
import { NO_ERRORS_SCHEMA } from '@angular/core'
import { LocalizePipe } from '../shared/pipe/localize'
import { AffiliationService } from './service/affiliations.service'
import { ReactiveFormsModule } from '@angular/forms'
import { of } from 'rxjs'
import { DateUtilService } from '../shared/service/date-util.service'

describe('AffiliationUpdateComponent', () => {
  let component: AffiliationUpdateComponent
  let fixture: ComponentFixture<AffiliationUpdateComponent>
  let affiliationService: jasmine.SpyObj<AffiliationService>
  let dateUtilService: jasmine.SpyObj<DateUtilService>

  beforeEach(() => {
    const affiliationServiceSpy = jasmine.createSpyObj('AffiliationService', ['update', 'create'])
    const dateUtilServiceSpy = jasmine.createSpyObj('DateUtilService', [
      'getCurrentMonthNumber',
      'getCurrentYear',
      'getYearsList',
      'getMonthsList',
      'getDaysList',
    ])

    TestBed.configureTestingModule({
      declarations: [AffiliationUpdateComponent, LocalizePipe],
      imports: [ReactiveFormsModule, HttpClientModule, RouterTestingModule.withRoutes([])],
      providers: [
        { provide: AffiliationService, useValue: affiliationServiceSpy },
        { provide: DateUtilService, useValue: dateUtilServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents()
    affiliationService = TestBed.inject(AffiliationService) as jasmine.SpyObj<AffiliationService>
    dateUtilService = TestBed.inject(DateUtilService) as jasmine.SpyObj<DateUtilService>
    fixture = TestBed.createComponent(AffiliationUpdateComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    dateUtilService.getDaysList.and.returnValue([])
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should initialize form with empty values', () => {
    expect(component.editForm.valid).toBeFalsy()
  })

  it('validate february 30 start date as false', () => {
    component.editForm.controls['startDay'].setValue(30)

    component.editForm.controls['startMonth'].setValue(2) // February
    component.onStartDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['startDay'].value).toEqual(null)
  })

  it('validate february 29 start date with a leap year as true', () => {
    component.editForm.controls['startYear'].setValue(2024)
    component.editForm.controls['startDay'].setValue(29)

    component.editForm.controls['startMonth'].setValue(2)
    component.onStartDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['startDay'].value).toEqual(29)
  })

  it('validate february 29 start date with a non-leap year as false', () => {
    component.editForm.controls['startYear'].setValue(2023)
    component.editForm.controls['startDay'].setValue(29)

    component.editForm.controls['startMonth'].setValue(2)
    component.onStartDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['startDay'].value).toEqual(null)
  })

  it('validate february 29 start date without a specified year as true', () => {
    component.editForm.controls['startDay'].setValue(29)

    component.editForm.controls['startMonth'].setValue(2)
    component.onStartDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['startDay'].value).toEqual(29)
  })

  it('validate february 30 end date as false', () => {
    component.editForm.controls['endDay'].setValue(30)

    component.editForm.controls['endMonth'].setValue(2) // February
    component.onEndDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['endDay'].value).toEqual(null)
  })

  it('validate february 29 end date with a leap year as true', () => {
    component.editForm.controls['endYear'].setValue(2024)
    component.editForm.controls['endDay'].setValue(29)

    component.editForm.controls['endMonth'].setValue(2)
    component.onEndDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['endDay'].value).toEqual(29)
  })

  it('validate february 29 end date with a non-leap year as false', () => {
    component.editForm.controls['endYear'].setValue(2023)
    component.editForm.controls['endDay'].setValue(29)

    component.editForm.controls['endMonth'].setValue(2)
    component.onEndDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['startDay'].value).toEqual(null)
  })

  it('validate february 29 end date without a specified year as true', () => {
    component.editForm.controls['endDay'].setValue(29)

    component.editForm.controls['endMonth'].setValue(2)
    component.onEndDateSelected(true)
    fixture.detectChanges()

    expect(component.editForm.controls['endDay'].value).toEqual(29)
  })

  it('start date cannot be greater than end date', () => {
    component.editForm.controls['startYear'].setValue(2024)
    component.editForm.controls['endDay'].setValue(28)
    component.editForm.controls['endMonth'].setValue(2)

    expect(component.editForm.get('endYear')?.hasError('dateValidator')).toBeFalsy()

    component.editForm.controls['startYear'].setValue(2023)
    component.editForm.controls['startDay'].setValue(28)
    component.editForm.controls['startMonth'].setValue(2)

    fixture.detectChanges()

    expect(component.editForm.get('endYear')?.hasError('dateValidator')).toBeTruthy()
  })
})
