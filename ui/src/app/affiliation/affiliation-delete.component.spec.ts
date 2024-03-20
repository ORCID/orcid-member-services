import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationDeletePopupComponent } from './affiliation-delete.component'
import { RouterTestingModule } from '@angular/router/testing'

describe('AffiliationDeleteComponent', () => {
  let component: AffiliationDeletePopupComponent
  let fixture: ComponentFixture<AffiliationDeletePopupComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffiliationDeletePopupComponent],
      imports: [RouterTestingModule],
    })
    fixture = TestBed.createComponent(AffiliationDeletePopupComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
