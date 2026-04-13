import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationDetailComponent } from './affiliation-detail.component'
import { UserService } from '../user/service/user.service'
import { RouterTestingModule } from '@angular/router/testing'
import { ActivatedRoute } from '@angular/router'
import { of } from 'rxjs'
import { NO_ERRORS_SCHEMA } from '@angular/core'
import { LocalizePipe } from '../shared/pipe/localize'

describe('AffiliationDetailComponent', () => {
  let component: AffiliationDetailComponent
  let fixture: ComponentFixture<AffiliationDetailComponent>
  let userServiceSpy: jasmine.SpyObj<UserService>

  const mockAffiliation = {
    startYear: '2020',
    startMonth: '01',
    startDay: '01',
    endYear: '2021',
    endMonth: '12',
    endDay: '31',
    ownerId: 'owner-id',
    created: new Date('2020-01-01'),
    modified: new Date('2021-01-01'),
  }

  beforeEach(() => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['find'])
    userServiceSpy.find.and.returnValue(of({ email: 'test@test.com' }))

    TestBed.configureTestingModule({
      declarations: [AffiliationDetailComponent, LocalizePipe],
      imports: [RouterTestingModule],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: ActivatedRoute, useValue: { data: of({ affiliation: mockAffiliation }) } },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(AffiliationDetailComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    userServiceSpy = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
