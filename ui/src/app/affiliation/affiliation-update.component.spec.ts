import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationUpdateComponent } from './affiliation-update.component'
import { HttpClientModule } from '@angular/common/http'
import { RouterTestingModule } from '@angular/router/testing'
import { NO_ERRORS_SCHEMA } from '@angular/core'
import { LocalizePipe } from '../shared/pipe/localize'

describe('AffiliationUpdateComponent', () => {
  let component: AffiliationUpdateComponent
  let fixture: ComponentFixture<AffiliationUpdateComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffiliationUpdateComponent, LocalizePipe],
      imports: [HttpClientModule, RouterTestingModule.withRoutes([])],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents()
    fixture = TestBed.createComponent(AffiliationUpdateComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
