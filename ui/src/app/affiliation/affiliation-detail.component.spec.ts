import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationDetailComponent } from './affiliation-detail.component'
import { UserService } from '../user/service/user.service'
import { RouterTestingModule } from '@angular/router/testing'

describe('AffiliationDetailComponent', () => {
  let component: AffiliationDetailComponent
  let fixture: ComponentFixture<AffiliationDetailComponent>
  let userServiceSpy: jasmine.SpyObj<UserService>

  beforeEach(() => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['find'])

    TestBed.configureTestingModule({
      declarations: [AffiliationDetailComponent],
      imports: [RouterTestingModule],
      providers: [{ provide: UserService, useValue: userServiceSpy }],
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
