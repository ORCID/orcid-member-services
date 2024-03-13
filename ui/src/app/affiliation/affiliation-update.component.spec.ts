import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationUpdateComponent } from './affiliation-update.component'
import { HttpClientModule } from '@angular/common/http'

describe('AffiliationUpdateComponent', () => {
  let component: AffiliationUpdateComponent
  let fixture: ComponentFixture<AffiliationUpdateComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffiliationUpdateComponent],
      imports: [HttpClientModule],
    })
    fixture = TestBed.createComponent(AffiliationUpdateComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
